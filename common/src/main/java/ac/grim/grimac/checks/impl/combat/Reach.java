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
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySizeable;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private final Int2ObjectMap<InteractionData> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    private boolean cancelImpossibleHits;
    private double threshold;
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

            // Dead entities cause false flags (https://github.com/GrimAnticheat/Grim/issues/546)
            if (entity.isDead) return;

            // TODO: Remove when in front of via
            if (entity.type == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_8))
                return;
            //Prevents Happy Ghast Reach false on 1.21.6+ servers with ViaBackwards set up
            if (entity.type == EntityTypes.HAPPY_GHAST && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6)) {
                return;
            }
            if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
                return;
            if (player.inVehicle()) return;
            if (entity.riding != null) return;

            InteractionHand hand = action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK ?
                    InteractionHand.MAIN_HAND : action.getHand(); // attacks can be only performed with the main hand
            ItemStack itemInHand = player.inventory.getItemInHand(hand);

            boolean tooManyAttacks = playerAttackQueue.size() > 10;
            if (!tooManyAttacks) {
                playerAttackQueue.put(action.getEntityId(), new InteractionData(new Vector3d(player.x, player.y, player.z), itemInHand)); // Queue for next tick for very precise check
            }

            boolean knownInvalid = isKnownInvalid(entity, itemInHand);

            if ((shouldModifyPackets() && cancelImpossibleHits && knownInvalid) || tooManyAttacks) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        // If the player set their look, or we know they have a new tick
        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle();
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
    private boolean isKnownInvalid(PacketEntity reachEntity, ItemStack itemInHand) {
        // If the entity doesn't exist, or if it is exempt, or if it is dead
        if ((blacklisted.contains(reachEntity.type) || !reachEntity.isLivingEntity) && reachEntity.type != EntityTypes.END_CRYSTAL)
            return false; // exempt

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return false;
        if (player.inVehicle()) return false;

        // Filter out what we assume to be cheats
        if (cancelBuffer != 0) {
            CheckResult result = checkReach(reachEntity, new Vector3d(player.x, player.y, player.z), itemInHand, true);
            return result.isFlag(); // If they flagged
        } else {
            SimpleCollisionBox targetBox = getTargetBox(reachEntity);

            double maxReach = applyReachModifiers(targetBox, itemInHand, !player.packetStateData.didLastMovementIncludePosition);

            return ReachUtils.getMinReachToBox(player, targetBox) > maxReach;
        }
    }

    private void tickBetterReachCheckWithAngle() {
        for (Int2ObjectMap.Entry<InteractionData> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            InteractionData interactionData = attack.getValue();
            CheckResult result = checkReach(reachEntity, interactionData.vector(), interactionData.itemInHand(), false);
            switch (result.type()) {
                case REACH -> {
                    String added = ", type=" + reachEntity.type.getName().getKey();
                    if (reachEntity instanceof PacketEntitySizeable sizeable) {
                        added += ", size=" + sizeable.size;
                    }
                    flagAndAlert(result.verbose() + added);
                }
                case HITBOX -> {
                    String added = "type=" + reachEntity.type.getName().getKey();
                    if (reachEntity instanceof PacketEntitySizeable sizeable) {
                        added += ", size=" + sizeable.size;
                    }
                    player.checkManager.getCheck(Hitboxes.class).flagAndAlert(result.verbose() + added);
                }
            }
        }

        playerAttackQueue.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, Vector3d from, ItemStack itemInHand, boolean isPrediction) {
        SimpleCollisionBox targetBox = getTargetBox(reachEntity);

        double maxReach = applyReachModifiers(targetBox, itemInHand, !player.packetStateData.didLastLastMovementIncludePosition);
        double minDistance = Double.MAX_VALUE;

        // https://bugs.mojang.com/browse/MC-67665
        List<Vector3dm> possibleLookDirs = new ArrayList<>(Collections.singletonList(ReachUtils.getLook(player, player.yaw, player.pitch)));

        // If we are a tick behind, we don't know their next look so don't bother doing this
        if (!isPrediction) {
            possibleLookDirs.add(ReachUtils.getLook(player, player.lastYaw, player.pitch));

            // 1.9+ players could be a tick behind because we don't get skipped ticks
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                possibleLookDirs.add(ReachUtils.getLook(player, player.lastYaw, player.lastPitch));
            }

            // 1.7 players do not have any of these issues! They are always on the latest look vector
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                possibleLookDirs = Collections.singletonList(ReachUtils.getLook(player, player.yaw, player.pitch));
            }
        }

        // +3 would be 3 + 3 = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = maxReach + 3;


        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        final Vector3dm eyePos = new Vector3dm(from.getX(), 0, from.getZ());
        for (Vector3dm lookVec : possibleLookDirs) {
            for (double eye : possibleEyeHeights) {
                eyePos.setY(from.getY() + eye);
                Vector3dm endReachPos = eyePos.clone().add(lookVec.getX() * distance, lookVec.getY() * distance, lookVec.getZ() * distance);

                Vector3dm intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                }
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.type) && reachEntity.isLivingEntity) || reachEntity.type == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MAX_VALUE) {
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

    private SimpleCollisionBox getTargetBox(PacketEntity reachEntity) {
        if (reachEntity.type == EntityTypes.END_CRYSTAL) { // Hardcode end crystal box
            return new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }
        return reachEntity.getPossibleCollisionBoxes();
    }

    private double applyReachModifiers(SimpleCollisionBox targetBox, ItemStack itemInHand, boolean giveMovementThreshold) {
        double maxReach;
        double hitboxMargin = threshold;

        ItemAttackRange attackRange = null;

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11) && ATTACK_RANGE_COMPONENT_EXISTS) {
            // TODO: ViaVersion support https://github.com/ViaVersion/ViaVersion/pull/4733
            attackRange = itemInHand.getComponentOr(ComponentTypes.ATTACK_RANGE, null);
        }

        if (attackRange != null) {
            maxReach = attackRange.getMaxRange();
            hitboxMargin += attackRange.getHitboxMargin();
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

    private static final boolean ATTACK_RANGE_COMPONENT_EXISTS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);

    @Override
    public void onReload(ConfigManager config) {
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.threshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }

    private enum ResultType {
        REACH, HITBOX, NONE
    }

    private record CheckResult(ResultType type, String verbose) {
        public boolean isFlag() {
            return type != ResultType.NONE;
        }
    }

    private record InteractionData(Vector3d vector, ItemStack itemInHand) {
    }

}
