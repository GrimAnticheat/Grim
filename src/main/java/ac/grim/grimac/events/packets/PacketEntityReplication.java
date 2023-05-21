package ac.grim.grimac.events.packets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHook;
import ac.grim.grimac.utils.data.packetentity.PacketEntityTrackXRot;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;

import java.util.ArrayList;
import java.util.List;

public class PacketEntityReplication extends Check implements PacketCheck {
    private boolean hasSentPreWavePacket = true;
    // Let's imagine the player is on a boat.
    // The player breaks this boat
    // If we were to despawn the boat without an extra transaction, then the boat would disappear before
    // it disappeared on the client side, creating a ghost boat to flag checks with
    //
    // If we were to despawn the tick after, spawning must occur the transaction before to stop the same exact
    // problem with ghost boats in reverse.
    //
    // Therefore, we despawn the transaction after, and spawn the tick before.
    //
    // If we despawn then spawn an entity in the same transaction, then this solution would despawn the new entity
    // instead of the old entity, so we wouldn't see the boat at all
    //
    // Therefore, if the server sends a despawn and then a spawn in the same transaction for the same entity,
    // We should simply add a transaction (which will clear this list!)
    //
    // Another valid solution is to simply spam more transactions, but let's not waste bandwidth.
    private final List<Integer> despawnedEntitiesThisTransaction = new ArrayList<>();

    public PacketEntityReplication(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Teleports don't interpolate, duplicate 1.17 packets don't interpolate
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;

            boolean isTickingReliably = player.isTickingReliablyFor(3);

            PacketEntity playerVehicle = player.compensatedEntities.getSelf().getRiding();
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity == playerVehicle && !player.vehicleData.lastDummy) {
                    // The player has this as their vehicle, so they aren't interpolating it.
                    // And it isn't a dummy position
                    entity.setPositionRaw(entity.getPossibleCollisionBoxes());
                } else {
                    entity.onMovement(isTickingReliably);
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PING || event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            despawnedEntitiesThisTransaction.clear();
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packetOutEntity = new WrapperPlayServerSpawnLivingEntity(event);
            addEntity(packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), packetOutEntity.getEntityMetadata(), 0);
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packetOutEntity = new WrapperPlayServerSpawnEntity(event);
            addEntity(packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), null, packetOutEntity.getData());
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packetOutEntity = new WrapperPlayServerSpawnPlayer(event);
            addEntity(packetOutEntity.getEntityId(), EntityTypes.PLAYER, packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), packetOutEntity.getEntityMetadata(), 0);
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove move = new WrapperPlayServerEntityRelativeMove(event);
            handleMoveEntity(event, move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), null, null, true, true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation move = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            handleMoveEntity(event, move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), move.getYaw() * 0.7111111F, move.getPitch() * 0.7111111F, true, true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport move = new WrapperPlayServerEntityTeleport(event);
            Vector3d pos = move.getPosition();
            handleMoveEntity(event, move.getEntityId(), pos.getX(), pos.getY(), pos.getZ(), move.getYaw(), move.getPitch(), false, true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION) { // Affects interpolation
            WrapperPlayServerEntityRotation move = new WrapperPlayServerEntityRotation(event);
            handleMoveEntity(event, move.getEntityId(), 0, 0, 0, move.getYaw() * 0.7111111F, move.getPitch() * 0.7111111F, true, false);
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.updateEntityMetadata(entityMetadata.getEntityId(), entityMetadata.getEntityMetadata()));
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect effect = new WrapperPlayServerEntityEffect(event);

            PotionType type = effect.getPotionType();

            // ViaVersion tries faking levitation effects and fails badly lol, flagging the anticheat
            // Block other effects just in case ViaVersion gets any ideas
            //
            // Set to 24 so ViaVersion blocks it
            // 24 is the levitation effect
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && ViaVersionUtil.isAvailable() && type.getId() > 23) {
                event.setCancelled(true);
                return;
            }

            // ViaVersion dolphin's grace also messes us up, set it to a potion effect that doesn't exist on 1.12
            // Effect 31 is bad omen
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_13) && ViaVersionUtil.isAvailable() && type.getId() == 30) {
                event.setCancelled(true);
                return;
            }

            if (isDirectlyAffectingPlayer(player, effect.getEntityId()))
                event.getTasksAfterSend().add(player::sendTransaction);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                PacketEntity entity = player.compensatedEntities.getEntity(effect.getEntityId());
                if (entity == null) return;

                entity.addPotionEffect(type, effect.getEffectAmplifier());
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrapperPlayServerRemoveEntityEffect effect = new WrapperPlayServerRemoveEntityEffect(event);

            if (isDirectlyAffectingPlayer(player, effect.getEntityId()))
                event.getTasksAfterSend().add(player::sendTransaction);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                PacketEntity entity = player.compensatedEntities.getEntity(effect.getEntityId());
                if (entity == null) return;

                entity.removePotionEffect(effect.getPotionType());
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrapperPlayServerUpdateAttributes attributes = new WrapperPlayServerUpdateAttributes(event);

            int entityID = attributes.getEntityId();

            // The attributes for this entity is active, currently
            if (isDirectlyAffectingPlayer(player, entityID)) player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> player.compensatedEntities.updateAttributes(entityID, attributes.getProperties()));
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
            WrapperPlayServerEntityStatus status = new WrapperPlayServerEntityStatus(event);
            // This hasn't changed from 1.7.2 to 1.17
            // Needed to exempt players on dead vehicles, as dead entities have strange physics.
            if (status.getStatus() == 3) {
                PacketEntity entity = player.compensatedEntities.getEntity(status.getEntityId());

                if (entity == null) return;
                entity.isDead = true;
            }

            if (status.getStatus() == 9) {
                if (status.getEntityId() != player.entityID) return;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
            }

            if (status.getStatus() == 31) {
                PacketEntity hook = player.compensatedEntities.getEntity(status.getEntityId());
                if (!(hook instanceof PacketEntityHook)) return;

                PacketEntityHook hookEntity = (PacketEntityHook) hook;
                if (hookEntity.attached == player.entityID) {
                    player.sendTransaction();
                    // We don't transaction sandwich this, it's too rare to be a real problem.
                    player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.uncertaintyHandler.fishingRodPulls.add(hookEntity.owner));
                }
            }

            if (status.getStatus() >= 24 && status.getStatus() <= 28 && status.getEntityId() == player.entityID) {
                player.compensatedEntities.getSelf().setOpLevel(status.getStatus() - 24);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);

            if (slot.getWindowId() == 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (slot.getSlot() - 36 == player.packetStateData.lastSlotSelected) {
                        player.packetStateData.slowedByUsingItem = false;
                    }
                });

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                    if (slot.getSlot() - 36 == player.packetStateData.lastSlotSelected) {
                        player.packetStateData.slowedByUsingItem = false;
                    }
                });
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);

            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
            }
        }

        // 1.8 clients fail to send the RELEASE_USE_ITEM packet when a window is opened client sided while using an item
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
        }
        if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers mount = new WrapperPlayServerSetPassengers(event);

            int vehicleID = mount.getEntityId();
            int[] passengers = mount.getPassengers();

            handleMountVehicle(event, vehicleID, passengers);
        }

        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            WrapperPlayServerAttachEntity attach = new WrapperPlayServerAttachEntity(event);

            // This packet was replaced by the mount packet on 1.9+ servers - to support multiple passengers on one vehicle
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;

            // If this is mounting rather than leashing
            if (!attach.isLeash()) {
                // Alright, let's convert this to the 1.9+ format to make it easier for grim
                int vehicleID = attach.getHoldingId();
                int attachID = attach.getAttachedId();
                TrackerData trackerData = player.compensatedEntities.getTrackedEntity(attachID);

                if (trackerData != null) {
                    // 1.8 sends a vehicle ID of -1 to dismount the entity from its vehicle
                    // This is opposite of the 1.9+ format, which sends the vehicle ID and then an empty array.
                    if (vehicleID == -1) { // Dismounting
                        vehicleID = trackerData.getLegacyPointEightMountedUpon();
                        handleMountVehicle(event, vehicleID, new int[]{}); // The vehicle is empty
                        return;
                    } else { // Mounting
                        trackerData.setLegacyPointEightMountedUpon(vehicleID);
                        handleMountVehicle(event, vehicleID, new int[]{attachID});
                    }
                } else {
                    // I don't think we can recover from this... warn and move on as this shouldn't happen.
                    LogUtil.warn("Server sent an invalid attach entity packet for entity " + attach.getHoldingId() + " with passenger " + attach.getAttachedId() + "! The client ignores this.");
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);

            int[] destroyEntityIds = destroy.getEntityIds();

            for (int entityID : destroyEntityIds) {
                despawnedEntitiesThisTransaction.add(entityID);
                player.compensatedEntities.serverPositionsMap.remove(entityID);
                // Remove the tracked vehicle (handling tracking knockback) if despawned
                if (player.compensatedEntities.serverPlayerVehicle != null && player.compensatedEntities.serverPlayerVehicle == entityID) {
                    player.compensatedEntities.serverPlayerVehicle = null;
                }
            }

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                for (int integer : destroyEntityIds) {
                    player.compensatedEntities.removeEntity(integer);
                    player.compensatedFireworks.removeFirework(integer);
                }
            });
        }
    }

    private void handleMountVehicle(PacketSendEvent event, int vehicleID, int[] passengers) {
        boolean wasInVehicle = player.getRidingVehicleId() == vehicleID;
        boolean inThisVehicle = false;

        for (int passenger : passengers) {
            inThisVehicle = passenger == player.entityID;
            if (inThisVehicle) break;
        }

        if (inThisVehicle && !wasInVehicle) {
            player.handleMountVehicle(vehicleID);
        }

        if (!inThisVehicle && wasInVehicle) {
            player.handleDismountVehicle(event);
        }
        // Better lag compensation if we were affected by this
        if (wasInVehicle || inThisVehicle) {
            player.sendTransaction();
        }
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            PacketEntity vehicle = player.compensatedEntities.getEntity(vehicleID);

            // Vanilla likes sending null vehicles, so we must ignore those like the client ignores them
            if (vehicle == null) return;

            // Eject existing passengers for this vehicle
            for (PacketEntity passenger : new ArrayList<>(vehicle.passengers)) {
                passenger.eject();
            }

            // Add the entities as vehicles
            for (int entityID : passengers) {
                PacketEntity passenger = player.compensatedEntities.getEntity(entityID);
                if (passenger == null) continue;
                passenger.mount(vehicle);
            }
        });
    }

    private void handleMoveEntity(PacketSendEvent event, int entityId, double deltaX, double deltaY, double deltaZ, Float yaw, Float pitch, boolean isRelative, boolean hasPos) {
        TrackerData data = player.compensatedEntities.getTrackedEntity(entityId);

        if (!hasSentPreWavePacket) {
            hasSentPreWavePacket = true;
            player.sendTransaction();
        }

        if (data != null) {
            // Update the tracked server's entity position
            if (isRelative) {
                // There is a bug where vehicles may start flying due to mojang setting packet position on the client
                // (Works at 0 ping but causes funny bugs at any higher ping)
                // As we don't want vehicles to fly, we need to replace it with a teleport if it is player vehicle
                //
                // Don't bother with client controlled vehicles though
                boolean vanillaVehicleFlight = player.compensatedEntities.serverPlayerVehicle != null && player.compensatedEntities.serverPlayerVehicle == entityId
                        && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) &&
                        PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);

                // ViaVersion sends two relative packets when moving more than 4 blocks
                // This is broken and causes the client to interpolate like (0, 4) and (1, 3) instead of (1, 7)
                // This causes impossible hits, so grim must replace this with a teleport entity packet
                // Not ideal, but neither is 1.8 players on a 1.9+ server.
                if (vanillaVehicleFlight ||
                        ((Math.abs(deltaX) >= 3.9375 || Math.abs(deltaY) >= 3.9375 || Math.abs(deltaZ) >= 3.9375) && player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9))) {
                    player.user.writePacket(new WrapperPlayServerEntityTeleport(entityId, new Vector3d(data.getX() + deltaX, data.getY() + deltaY, data.getZ() + deltaZ), yaw == null ? data.getXRot() : yaw, pitch == null ? data.getYRot() : pitch, false));
                    event.setCancelled(true);
                    return;
                }

                data.setX(data.getX() + deltaX);
                data.setY(data.getY() + deltaY);
                data.setZ(data.getZ() + deltaZ);
            } else {
                data.setX(deltaX);
                data.setY(deltaY);
                data.setZ(deltaZ);
            }
            if (yaw != null) {
                data.setXRot(yaw);
                data.setYRot(pitch);
            }

            // We can't hang two relative moves on one transaction
            if (data.getLastTransactionHung() == player.lastTransactionSent.get()) {
                player.sendTransaction();
            }
            data.setLastTransactionHung(player.lastTransactionSent.get());
        }

        int lastTrans = player.lastTransactionSent.get();

        player.latencyUtils.addRealTimeTask(lastTrans, () -> {
            PacketEntity entity = player.compensatedEntities.getEntity(entityId);
            if (entity == null) return;
            if (entity instanceof PacketEntityTrackXRot && yaw != null) {
                PacketEntityTrackXRot xRotEntity = (PacketEntityTrackXRot) entity;
                xRotEntity.packetYaw = yaw;
                xRotEntity.steps = EntityTypes.isTypeInstanceOf(entity.type, EntityTypes.BOAT) ? 10 : 3;
            }
            entity.onFirstTransaction(isRelative, hasPos, deltaX, deltaY, deltaZ, player);
        });
        player.latencyUtils.addRealTimeTask(lastTrans + 1, () -> {
            PacketEntity entity = player.compensatedEntities.getEntity(entityId);
            if (entity == null) return;
            entity.onSecondTransaction();
        });
    }

    public void addEntity(int entityID, EntityType type, Vector3d position, float xRot, float yRot, List<EntityData> entityMetadata, int extraData) {
        if (despawnedEntitiesThisTransaction.contains(entityID)) {
            player.sendTransaction();
        }

        player.compensatedEntities.serverPositionsMap.put(entityID, new TrackerData(position.getX(), position.getY(), position.getZ(), xRot, yRot, type, player.lastTransactionSent.get()));

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            player.compensatedEntities.addEntity(entityID, type, position, xRot, extraData);
            if (entityMetadata != null) {
                player.compensatedEntities.updateEntityMetadata(entityID, entityMetadata);
            }
        });
    }

    private boolean isDirectlyAffectingPlayer(GrimPlayer player, int entityID) {
        // The attributes for this entity is active, currently
        return (player.compensatedEntities.serverPlayerVehicle == null && entityID == player.entityID) ||
                (player.compensatedEntities.serverPlayerVehicle != null && entityID == player.compensatedEntities.serverPlayerVehicle);
    }

    public void onEndOfTickEvent() {
        // Only send a transaction at the end of the tick if we are tracking players
        player.sendTransaction(true); // We injected before vanilla flushes :) we don't need to flush
    }

    public void tickStartTick() {
        hasSentPreWavePacket = false;
    }
}
