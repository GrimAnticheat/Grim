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
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.debug.HitboxDebugHandler;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BlockHitData;
import ac.grim.grimac.utils.data.EntityHitData;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import ac.grim.grimac.utils.nmsutil.WorldRayTrace;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// You may not copy the check unless you are licensed under GPL
@CheckData(name = "Reach", setback = 10)
public class Reach extends Check implements PacketCheck {

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);
    private static final CheckResult NONE = new CheckResult(ResultType.NONE, "");
    // Only one flag per reach attack, per entity, per tick.
    // We store position because lastX isn't reliable on teleports.
    private final Int2ObjectMap<Vector3d> playerAttackQueue = new Int2ObjectOpenHashMap<>();
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
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);

            // Don't let the player teleport to bypass reach
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
                player.onPacketCancel();
                return;
            }

            PacketEntity entity = player.compensatedEntities.entityMap.get(action.getEntityId());
            // Stop people from freezing transactions before an entity spawns to bypass reach
            // TODO: implement dragon parts?
            if (entity == null || entity instanceof PacketEntityEnderDragonPart) {
                // Only cancel if and only if we are tracking this entity
                // This is because we don't track paintings.
                if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(action.getEntityId())) {
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

            if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
                return;
            if (player.inVehicle()) return;
            if (entity.riding != null) return;

            boolean tooManyAttacks = playerAttackQueue.size() > 10;
            if (!tooManyAttacks) {
                playerAttackQueue.put(action.getEntityId(), new Vector3d(player.x, player.y, player.z)); // Queue for next tick for very precise check
            }

            boolean knownInvalid = isKnownInvalid(entity);

            if ((shouldModifyPackets() && cancelImpossibleHits && knownInvalid) || tooManyAttacks) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        // If the player set their look, or we know they have a new tick
        final boolean isFlying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle(isFlying);
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
    private boolean isKnownInvalid(PacketEntity reachEntity) {
        // If the entity doesn't exist, or if it is exempt, or if it is dead
        if ((blacklisted.contains(reachEntity.getType()) || !reachEntity.isLivingEntity()) && reachEntity.getType() != EntityTypes.END_CRYSTAL)
            return false; // exempt

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return false;
        if (player.inVehicle()) return false;

        // Filter out what we assume to be cheats
        if (cancelBuffer != 0) {
            CheckResult result = checkReach(reachEntity, new Vector3d(player.x, player.y, player.z), true);
            return result.isFlag(); // If they flagged
        } else {
            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();
            if (reachEntity.getType() == EntityTypes.END_CRYSTAL) {
                targetBox = new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
            }
            return ReachUtils.getMinReachToBox(player, targetBox) > player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        }
    }

    private void tickBetterReachCheckWithAngle(boolean isFlying) {
        for (Int2ObjectMap.Entry<Vector3d> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            CheckResult result = checkReach(reachEntity, attack.getValue(), false);
            switch (result.type()) {
                case REACH -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : ", type=" + reachEntity.getType().getName().getKey();
                    flagAndAlert(result.verbose() + added);
                }
                case HITBOX -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getCheck(Hitboxes.class).flagAndAlert(result.verbose() + added);
                }
                case WALL_HIT -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getCheck(WallHit.class).flagAndAlert(result.verbose() + added);
                }
                case ENTITY_PIERCE -> {
                    String added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getCheck(EntityPierce.class).flagAndAlert(result.verbose() + added);
                }
            }
        }

        playerAttackQueue.clear();
        // We can't use transactions for this because of this problem:
        // transaction -> block changed applied -> 2nd transaction -> list cleared -> attack packet -> flying -> reach block hit checked, falses
        if (isFlying) blocksChangedThisTick.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, Vector3d from, boolean isPrediction) {
        SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();

        if (reachEntity.getType() == EntityTypes.END_CRYSTAL) { // Hardcode end crystal box
            targetBox = new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }

        // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
        // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            targetBox.expand(0.1f);
        }

        targetBox.expand(threshold);

        // This is better than adding to the reach, as 0.03 can cause a player to miss their target
        // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
        //
        // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
        // However, on 1.21.2+ we do know if they had 0.03 movement
        if (!player.packetStateData.didLastLastMovementIncludePosition || player.canSkipTicks())
            targetBox.expand(player.getMovementThreshold());

        double minDistance = Double.MAX_VALUE;

        // will store all lookVecsAndEyeHeight pairs that landed a hit on the target entity
        // We only need to check for blocking intersections for these
        List<Pair<Vector3dm, Double>> lookVecsAndEyeHeights = new ArrayList<>();

        final double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        // +3 would be 3 + 3 = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = maxReach + 3;
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        final Vector3dm[] possibleLookDirs = player.getPossibleLookVectors(isPrediction);
        final Vector3dm eyePos = new Vector3dm(from.getX(), 0, from.getZ());
        for (Vector3dm lookVec : possibleLookDirs) {
            for (double eye : possibleEyeHeights) {
                eyePos.setY(from.getY() + eye);
                Vector3dm endReachPos = eyePos.clone().add(new Vector3dm(lookVec.getX() * distance, lookVec.getY() * distance, lookVec.getZ() * distance));

                Vector3dm intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

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

        if (hitboxDebuggingEnabled())
            sendHitboxDebugData(reachEntity, from, lookVecsAndEyeHeights, isPrediction);

        HitData foundHitData = null;
        // If the entity is within range of the player (we'll flag anyway if not, so no point checking blocks in this case)
        // Ignore when could be hitting through a moving shulker, piston blocks. They are just too glitchy/uncertain to check.
        if (minDistance <= distance - extraSearchDistance && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) {
            // we can optimize didRayTraceHit more to only rayTrace up to the maximize distance of all rays that hit to the target...
            // I'm too lazy to do that and we don't need to optimize that much yet so...
            final @Nullable Pair<Double, HitData> hitResult = WorldRayTrace.didRayTraceHit(player, reachEntity, lookVecsAndEyeHeights, from);
            HitData hitData = hitResult.second();
            // If the returned hit result was NOT the target entity we flag the check
            if (hitData instanceof EntityHitData &&
                    player.compensatedEntities.getPacketEntityID(((EntityHitData) hitData).getEntity()) != player.compensatedEntities.getPacketEntityID(reachEntity)) {
                minDistance = Double.MIN_VALUE;
                foundHitData = hitData;
                // until I fix block modeling exempt any blocks changed this tick
            } else if (hitData instanceof BlockHitData && !blocksChangedThisTick.contains(((BlockHitData) hitData).getPosition())) {
                minDistance = Double.MIN_VALUE;
                foundHitData = hitData;
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity()) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MIN_VALUE && foundHitData != null) {
                cancelBuffer = 1;
                if (foundHitData instanceof BlockHitData) {
                    return new CheckResult(ResultType.WALL_HIT, "Hit block=" + ((BlockHitData) foundHitData).getState().getType().getName() + " ");
                } else { // entity hit data
                    return new CheckResult(ResultType.ENTITY_PIERCE, "Hit entity=" + ((EntityHitData) foundHitData).getEntity().getType().getName() + " ");
                }
            } else if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.HITBOX, "");
            } else if (minDistance > maxReach) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.REACH, String.format("%.5f", minDistance) + " blocks");
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return NONE;
    }

    @Override
    public void onReload(ConfigManager config) {
        this.ignoreNonPlayerTargets = config.getBooleanElse("Reach.ignore-non-player-targets", false);
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.threshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }

    private enum ResultType {
        REACH, HITBOX, WALL_HIT, ENTITY_PIERCE, NONE
    }

    private record CheckResult(ResultType type, String verbose) {
        public boolean isFlag() {
            return type != ResultType.NONE;
        }
    }

    public void handleBlockChange(Vector3i vector3i, WrappedBlockState state) {
        if (blocksChangedThisTick.size() >= 40) return; // Don't let players freeze movement packets to grow this
        // Only do this for nearby blocks
        if (new Vector3dm(vector3i.x, vector3i.y, vector3i.z).distanceSquared(new Vector3dm(player.x, player.y, player.z)) > 6) return;
        // Only do this if the state really had any world impact
        if (state.equals(player.compensatedWorld.getBlock(vector3i))) return;
        blocksChangedThisTick.add(vector3i);
    }

    private boolean hitboxDebuggingEnabled() {
        return player.checkManager.getCheck(HitboxDebugHandler.class).isEnabled();
    }

    private void sendHitboxDebugData(PacketEntity reachEntity, Vector3d from, List<Pair<Vector3dm, Double>> lookVecsAndEyeHeights, boolean isPrediction) {
        Map<Integer, CollisionBox> hitboxes = new HashMap<>();
        for (Int2ObjectMap.Entry<PacketEntity> entry : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
            PacketEntity entity = entry.getValue();
            if (!entity.canHit()) continue;

            CollisionBox box;

            if (entity.equals(reachEntity)) {
                // Target entity gets expanded hitbox
                box = entity.getPossibleCollisionBoxes();
                SimpleCollisionBox sBox = (SimpleCollisionBox) box;
                sBox.expand(threshold);

                // Add movement threshold uncertainty for 1.9+ or non-position updates
                if (!player.packetStateData.didLastLastMovementIncludePosition
                        || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    sBox.expand(player.getMovementThreshold());
                }
            } else {
                // Non-target entities
                box = entity.getMinimumPossibleCollisionBoxes();
                if (box instanceof NoCollisionBox) {
                    hitboxes.put(entry.getIntKey(), NoCollisionBox.INSTANCE);
                    continue;
                } else if (box instanceof SimpleCollisionBox) {
                    SimpleCollisionBox sBox = (SimpleCollisionBox) box;
                    sBox.expand(-threshold);
                    // Shrink non-target entities by movement threshold when applicable
                    if (!player.packetStateData.didLastLastMovementIncludePosition
                            || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                        sBox.expand(-player.getMovementThreshold());
                    }
                }
            }

            // Add 1.8 and below extra hitbox size
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                    && box instanceof SimpleCollisionBox) {
                ((SimpleCollisionBox) box).expand(0.1f);
            }

            hitboxes.put(entry.getIntKey(), box);
        }

        player.checkManager.getCheck(HitboxDebugHandler.class).sendHitboxData(hitboxes,
                Collections.singleton(player.compensatedEntities.getPacketEntityID(reachEntity)),
                lookVecsAndEyeHeights,
                new Vector3dm(from.getX(), from.getY(), from.getZ()),
                isPrediction, player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
    }
}
