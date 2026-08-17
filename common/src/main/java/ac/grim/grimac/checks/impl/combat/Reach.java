// This file was designed and is an original check for GrimAC
// Copyright (C) 2021 DefineOutside
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BlockHitData;
import ac.grim.grimac.utils.data.EntityHitData;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySizeable;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import ac.grim.grimac.utils.nmsutil.WorldRayTrace;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttackRange;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.viaversion.viaversion.api.Via;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// You may not copy the check unless you are licensed under GPL
@CheckData(name = "Reach", stableKey = "grim.combat.reach", description = "Attacked an entity from too far away", setback = 10)
public class Reach extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("{f64:%.5f} blocks, type={entity}");

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);
    private static final CheckResult NONE = new CheckResult(ResultType.NONE, 0, 0, false);
    // Only one flag per reach attack, per entity, per tick.
    // We store position because lastX isn't reliable on teleports.
    private final Int2ObjectMap<InteractionData> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    // temporarily used to prevent falses in the wall hit check
    private final Set<Vector3i> blocksChangedThisTick = new HashSet<>();
    // extra distance to raytrace beyond player reach distance so we know how far beyond the legit distance a cheater hit
    public static final double extraSearchDistance = 3;

    private boolean ignoreNonPlayerTargets;
    private boolean cancelImpossibleHits;
    public double threshold;
    private double cancelBuffer; // For the next 4 hits after using reach, we aggressively cancel reach

    public Reach(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId(), InteractionHand.MAIN_HAND);
        }

        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            onInteract(event, packet.getEntityId(), packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK ? InteractionHand.MAIN_HAND : packet.getHand());
        }

        // If the player set their look, or we know they have a new tick
        final boolean isFlying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle(isFlying);
        }
    }

    private void onInteract(PacketReceiveEvent event, int entityId, InteractionHand hand) {
        // Don't let the player teleport to bypass reach
        if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        // Stop people from freezing transactions before an entity spawns to bypass reach
        // TODO: implement dragon parts?
        if (entity == null || entity instanceof PacketEntityEnderDragonPart) {
            // Only cancel if and only if we are tracking this entity
            // This is because we don't track paintings.
            if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(entityId)) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        if (ignoreNonPlayerTargets && !entity.getType().equals(EntityTypes.PLAYER)) {
            return;
        }

        // Dead entities cause false flags (https://github.com/GrimAnticheat/Grim/issues/546)
        if (entity.isDead) return;

        // TODO: Remove when in front of via
        if (entity.getType() == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_8))
            return;
        // Prevents Happy Ghast Reach false on 1.21.6+ servers with ViaBackwards set up
        if (entity.getType() == EntityTypes.HAPPY_GHAST && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6))
            return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return;
        if (player.inVehicle()) return;
        if (entity.riding != null) return;

        ItemStack currentStack = player.inventory.getItemInHand(hand);
        ItemStack startStack = player.inventory.getStartOfTickStack();

        boolean hasRange = false;
        float maxReach = 0f;
        float hitboxMargin = 0f;
        Vector3dm attackRangeMovement = null;

        boolean clientAttackRangeExists = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        boolean clientAndServerAgrees = clientAttackRangeExists && ATTACK_RANGE_COMPONENT_EXISTS;

        boolean viaVersionAvailable = false;
        if (USE_1_8_HITBOX_MARGIN && ViaVersionUtil.isAvailable) {
            viaVersionAvailable = Via.getConfig().getValues().containsKey("use-1_8-hitbox-margin") && Via.getConfig().use1_8HitboxMargin();
        }

        boolean clientAndViaVersion = clientAttackRangeExists && viaVersionAvailable;
        if (clientAndServerAgrees || clientAndViaVersion) {
            ItemAttackRange startRange = startStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);
            ItemAttackRange currentRange = currentStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);

            if (clientAndViaVersion) {
                if (startStack != ItemStack.EMPTY) {
                    startRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }

                if (currentStack != ItemStack.EMPTY) {
                    currentRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }
            }

            // If the start stack has no range component, the client defaults to vanilla reach behavior,
            // regardless of what the current stack is (No Range -> X = No Range used).
            if (startRange != null) {
                hasRange = true;
                if (currentRange == null) {
                    // Range (Start) -> No Range (Current)
                    // Client logic uses Start Range, including the attack_range movement projection.
                    if (!clientAndViaVersion) {
                        attackRangeMovement = player.clientVelocity.clone();
                    }
                    maxReach = startRange.getMaxRange();
                    hitboxMargin = startRange.getHitboxMargin();
                } else {
                    // Range (Start) -> Range (Current)
                    // Client logic requires satisfying BOTH constraints
                    maxReach = Math.min(startRange.getMaxRange(), currentRange.getMaxRange());
                    hitboxMargin = Math.min(startRange.getHitboxMargin(), currentRange.getHitboxMargin());
                }
            }
        }

        boolean tooManyAttacks = playerAttackQueue.size() > 10;
        if (!tooManyAttacks) {
            playerAttackQueue.put(entityId, new InteractionData(
                    player.x, player.y, player.z,
                    hasRange, maxReach, hitboxMargin, attackRangeMovement
            )); // Queue for next tick for very precise check
        }

        boolean knownInvalid = attackRangeMovement == null && isKnownInvalid(entity, hasRange, maxReach, hitboxMargin);

        if ((shouldModifyPackets() && cancelImpossibleHits && knownInvalid) || tooManyAttacks) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    // This method finds the most optimal point at which the user should be aiming at
    // and then measures the distance between the player's eyes and this target point
    //
    // It will not cancel every invalid attack but should cancel 3.05+ or so in real-time
    // Let the post look check measure the distance, as it will always return equal or higher
    // than this method.  If this method flags, the other method WILL flag.
    //
    // Meaning that the other check should be the only one that flags.
    private boolean isKnownInvalid(PacketEntity reachEntity, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin) {
        // If the entity doesn't exist, or if it is exempt, or if it is dead
        if ((blacklisted.contains(reachEntity.getType()) || !reachEntity.isLivingEntity) && reachEntity.getType() != EntityTypes.END_CRYSTAL)
            return false; // exempt

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return false;
        if (player.inVehicle()) return false;

        // Filter out what we assume to be cheats
        if (cancelBuffer != 0) {
            CheckResult result = checkReach(reachEntity, player.x, player.y, player.z, hasAttackRange, itemMaxReach, itemHitboxMargin, null, true);
            return result.isFlag();
        } else {
            SimpleCollisionBox targetBox = getTargetBox(reachEntity);

            double maxReach = applyReachModifiers(targetBox, hasAttackRange, itemMaxReach, itemHitboxMargin, !player.packetStateData.didLastMovementIncludePosition);
            return ReachUtils.getMinReachToBox(player, targetBox) > maxReach;
        }
    }

    private void tickBetterReachCheckWithAngle(boolean isFlying) {
        for (Int2ObjectMap.Entry<InteractionData> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            InteractionData interactionData = attack.getValue();
            CheckResult result = checkReach(reachEntity, interactionData.x, interactionData.y, interactionData.z, interactionData.hasAttackRange, interactionData.maxReach, interactionData.hitboxMargin, interactionData.attackRangeMovement, false);
            switch (result.type()) {
                case REACH -> {
                    flag(
                            V.write(verbose()).f64(result.minDistance()).uint(reachEntity.getType().getId(player.getClientVersion())),
                            () -> {
                                String added = ", type=" + reachEntity.getType().getName().getKey();
                                if (reachEntity instanceof PacketEntitySizeable sizeable) {
                                    added += ", size=" + sizeable.size;
                                }
                                return result.verbose() + added;
                            });
                }
                case HITBOX -> {
                    String added = "type=" + reachEntity.getType().getName().getKey();
                    if (reachEntity instanceof PacketEntitySizeable sizeable) {
                        added += ", size=" + sizeable.size;
                    }
                    player.checkManager.getCheck(Hitboxes.class).flag(result.verbose() + added);
                }
                case WALL_HIT -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getCheck(WallHit.class).flag(result.verbose() + added);
                }
                case ENTITY_PIERCE -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getCheck(EntityPierce.class).flag(result.verbose() + added);
                }
            }
        }

        playerAttackQueue.clear();
        // We can't use transactions for this because of this problem:
        // transaction -> block changed applied -> 2nd transaction -> list cleared -> attack packet -> flying -> reach block hit checked, falses
        if (isFlying) blocksChangedThisTick.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, double x, double y, double z, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin, Vector3dm attackRangeMovement, boolean isPrediction) {
        SimpleCollisionBox targetBox = getTargetBox(reachEntity);

        double movementAllowance = attackRangeMovement == null ? 0 : getAttackRangeMovementAllowance(attackRangeMovement);
        double maxReach = applyReachModifiers(targetBox, hasAttackRange, itemMaxReach, itemHitboxMargin, !player.packetStateData.didLastLastMovementIncludePosition) + movementAllowance;
        double minDistance = Double.MAX_VALUE;

        // https://bugs.mojang.com/browse/MC-67665
        // will store all lookVecsAndEyeHeight pairs that landed a hit on the target entity
        // We only need to check for blocking intersections for these
        List<Pair<Vector3dm, Double>> lookVecsAndEyeHeights = new ArrayList<>();

        // +3 would be 3 + 3 = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = maxReach + 3;

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        final Vector3dm[] possibleLookDirs = player.getPossibleLookVectors(isPrediction);
        for (Vector3dm lookVec : possibleLookDirs) {
            for (double eye : possibleEyeHeights) {
                final Vector3d eyePos = new Vector3d(x, y + eye, z);
                Vector3d endReachPos = eyePos.add(lookVec.getX() * distance, lookVec.getY() * distance, lookVec.getZ() * distance);

                Vector3d intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                    lookVecsAndEyeHeights.add(new Pair<>(lookVec, eye));
                }
            }
        }

        HitData foundHitData = null;
        // If the entity is within range of the player (we'll flag anyway if not, so no point checking blocks in this case)
        // Ignore when could be hitting through a moving shulker, piston blocks. They are just too glitchy/uncertain to check.
        if (minDistance <= distance - extraSearchDistance && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) {
            // we can optimize didRayTraceHit to more only rayTrace up to the maximize distance of all rays that hit to the target...
            // I'm too lazy to do that and we don't need to optimize that much yet so...
            final Pair<Double, HitData> hitResult = WorldRayTrace.didRayTraceHit(player, reachEntity, lookVecsAndEyeHeights, x, y, z);
            HitData hitData = hitResult.second();
            // If the returned hit result was NOT the target entity we flag the check
            if (hitData instanceof EntityHitData &&
                    player.compensatedEntities.getPacketEntityID(((EntityHitData) hitData).getEntity()) != player.compensatedEntities.getPacketEntityID(reachEntity)) {
                minDistance = Double.MIN_VALUE;
                foundHitData = hitData;
                // until I fix block modeling exempt any blocks changed this tick
            } else if (hitData instanceof BlockHitData && !blocksChangedThisTick.contains(((BlockHitData) hitData).position())) {
                minDistance = Double.MIN_VALUE;
                foundHitData = hitData;
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MIN_VALUE && foundHitData != null) {
                cancelBuffer = 1;
                if (foundHitData instanceof BlockHitData) {
                    return new CheckResult(ResultType.WALL_HIT, 0, 0, false, "Hit block=" + ((BlockHitData) foundHitData).state().getType().getName() + " ");
                } else { // entity hit data
                    return new CheckResult(ResultType.ENTITY_PIERCE, 0, 0, false, "Hit entity=" + ((EntityHitData) foundHitData).getEntity().getType().getName() + " ");
                }
            } else if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.HITBOX, 0, 0, false);
            } else if (minDistance > maxReach) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.REACH, minDistance, movementAllowance, attackRangeMovement != null);
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return NONE;
    }

    private SimpleCollisionBox getTargetBox(PacketEntity reachEntity) {
        if (reachEntity.getType() == EntityTypes.END_CRYSTAL) { // Hardcode end crystal box
            return new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }
        return reachEntity.getPossibleCollisionBoxes();
    }

    private double getAttackRangeMovementAllowance(Vector3dm attackRangeMovement) {
        if (attackRangeMovement.lengthSquared() == 0) {
            return 0;
        }

        return getForwardMovement(attackRangeMovement, player.yaw, player.pitch);
    }

    private double getForwardMovement(Vector3dm movement, float yaw, float pitch) {
        return Math.max(0, movement.dot(ReachUtils.getLook(player, yaw, pitch)));
    }

    private static final boolean ATTACK_RANGE_COMPONENT_EXISTS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);
    private static final boolean USE_1_8_HITBOX_MARGIN = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);

    private double applyReachModifiers(SimpleCollisionBox targetBox, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin, boolean giveMovementThreshold) {
        double maxReach;
        double hitboxMargin = threshold;

        if (hasAttackRange) {
            maxReach = itemMaxReach;
            hitboxMargin += itemHitboxMargin;
        } else {
            maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
            // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                hitboxMargin += 0.1f;
            }
        }

        // This is better than adding to the reach, as 0.03 can cause a player to miss their target
        // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
        //
        // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
        // However, on 1.21.2+ we do know if they had 0.03 movement
        if (giveMovementThreshold || player.canSkipTicks()) {
            hitboxMargin += player.getMovementThreshold();
        }

        targetBox.expand(hitboxMargin);

        return maxReach;
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        this.ignoreNonPlayerTargets = config.getBooleanElse("Reach.ignore-non-player-targets", false);
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.threshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }

    private enum ResultType {
        REACH, HITBOX, WALL_HIT, ENTITY_PIERCE, NONE
    }

    private record CheckResult(ResultType type, double minDistance, double extraMovement, boolean hasExtraMovement, String message) {
        private CheckResult(ResultType type, double minDistance, double extraMovement, boolean hasExtraMovement) {
            this(type, minDistance, extraMovement, hasExtraMovement, "");
        }

        public boolean isFlag() {
            return type != ResultType.NONE;
        }

        public String verbose() {
            if (!message.isEmpty()) {
                return message;
            }
            if (type != ResultType.REACH) {
                return "";
            }

            String verbose = String.format("%.5f", minDistance) + " blocks";
            if (hasExtraMovement) {
                verbose += String.format(", extraMovement=%.5f", extraMovement);
            }
            return verbose;
        }
    }

    public void handleBlockChange(Vector3i vector3i, WrappedBlockState state) {
        if (blocksChangedThisTick.size() >= 40) return; // Don't let players freeze movement packets to grow this
        // Only do this for nearby blocks
        if (GrimMath.distanceSquared(vector3i.x, vector3i.y, vector3i.z, player.x, player.y, player.z) > 36) return;
        // Only do this if the state really had any world impact
        if (state.equals(player.compensatedWorld.getBlock(vector3i))) return;
        blocksChangedThisTick.add(vector3i);
    }

    private record InteractionData(double x, double y, double z, boolean hasAttackRange,
                                   float maxReach, float hitboxMargin, Vector3dm attackRangeMovement) {}
}
