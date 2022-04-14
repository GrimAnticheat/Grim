package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.ServerPacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
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
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;

import java.util.ArrayList;
import java.util.List;

public class PacketEntityReplication extends PacketCheck {

    private final boolean enablePreWavePacket;
    private boolean hasSentPreWavePacket = true;

    public PacketEntityReplication(GrimPlayer player) {
        super(player);
        enablePreWavePacket = GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("reach.enable-pre-packet", false);
    }

    public void tickFlying() {
        boolean setHighBound = !player.inVehicle && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            entity.onMovement(setHighBound);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Teleports don't interpolate, duplicate 1.17 packets don't interpolate
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;
            tickFlying();
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packetOutEntity = new WrapperPlayServerSpawnLivingEntity(event);
            addEntity(event.getUser(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), null, packetOutEntity.getEntityMetadata());
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packetOutEntity = new WrapperPlayServerSpawnEntity(event);
            addEntity(event.getUser(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), packetOutEntity.getData(), null);
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packetOutEntity = new WrapperPlayServerSpawnPlayer(event);
            addEntity(event.getUser(), packetOutEntity.getEntityId(), EntityTypes.PLAYER, packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), null, packetOutEntity.getEntityMetadata());
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
            ServerPacketEntity serverPacketEntity = player.compensatedEntities.serverEntityMap.get(entityMetadata.getEntityId());
            if (serverPacketEntity != null) {
                serverPacketEntity.setMetadata(entityMetadata.getEntityMetadata());
            }
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.updateEntityMetadata(entityMetadata.getEntityId(), entityMetadata.getEntityMetadata()));
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect effect = new WrapperPlayServerEntityEffect(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

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
                event.getPostTasks().add(player::sendTransaction);

            player.compensatedPotions.addPotionEffect(type, effect.getEffectAmplifier(), effect.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrapperPlayServerRemoveEntityEffect effect = new WrapperPlayServerRemoveEntityEffect(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (isDirectlyAffectingPlayer(player, effect.getEntityId()))
                event.getPostTasks().add(player::sendTransaction);

            player.compensatedPotions.removePotionEffect(effect.getPotionType(), effect.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_PROPERTIES) {
            WrapperPlayServerEntityProperties attributes = new WrapperPlayServerEntityProperties(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

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
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                PacketEntity entity = player.compensatedEntities.getEntity(status.getEntityId());

                if (entity == null) return;
                entity.isDead = true;
            }

            if (status.getStatus() == 9) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                if (status.getEntityId() != player.entityID) return;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
            }

            if (status.getStatus() == 31) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                ServerPacketEntity fishingRod = player.compensatedEntities.serverEntityMap.get(status.getEntityId());
                if (fishingRod == null) return;

                if (player.entityID != (int) fishingRod.getMetadata().get(0).getValue() - 1) return; // Only send the packet to the person affected

                event.setCancelled(true); // We replace this packet with an explosion packet

                ServerPacketEntity owner = player.compensatedEntities.serverEntityMap.get(fishingRod.getData());
                // Hide the explosion noise
                // going too far will cause a memory leak in the client
                // So 256 blocks is good enough and far past the minimum 16 blocks away we need to be for no sound
                Vector3f pos = new Vector3f((float) owner.getX(), (float) (owner.getY() - 256), (float) owner.getZ());

                // Exact calculation
                Vector3d diff = new Vector3d(owner.getX(), owner.getY(), owner.getZ()).subtract(player.x, player.y, player.z);
                Vector3f diffF = new Vector3f((float) (diff.getX()*0.1), (float) (diff.getY()*0.1), (float) (diff.getZ()*0.1));

                WrapperPlayServerExplosion explosion = new WrapperPlayServerExplosion(pos, 0, new ArrayList<>(), diffF);
                // There we go, this is how you implement this packet correctly, Mojang.
                // Please stop being so stupid.
                player.user.sendPacket(explosion);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

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

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers mount = new WrapperPlayServerSetPassengers(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            int vehicleID = mount.getEntityId();
            int[] passengers = mount.getPassengers();

            handleMountVehicle(event, vehicleID, passengers);
        }

        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            WrapperPlayServerAttachEntity attach = new WrapperPlayServerAttachEntity(event);

            // This packet was replaced by the mount packet on 1.9+ servers - to support multiple passengers on one vehicle
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // If this is mounting rather than leashing
            if (!attach.isLeash()) {
                handleMountVehicle(event, attach.getHoldingId(), new int[]{attach.getAttachedId()});
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            int[] destroyEntityIds = destroy.getEntityIds();

            for (int entityID : destroyEntityIds) {
                player.compensatedEntities.serverEntityMap.remove(entityID);
            }

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                for (int integer : destroyEntityIds) {
                    player.compensatedEntities.entityMap.remove(integer);
                    player.compensatedFireworks.removeFirework(integer);
                    player.compensatedPotions.removeEntity(integer);
                    // Remove player vehicle if it despawns
                    if (player.vehicle != null && player.vehicle == integer) {
                        player.vehicle = null;
                        player.playerVehicle = null;
                        player.inVehicle = false;
                        player.vehicleData.wasVehicleSwitch = true;
                    }
                }
            });
        }
    }

    private void handleMountVehicle(PacketSendEvent event, int vehicleID, int[] passengers) {
        boolean wasInVehicle = player.compensatedEntities.serverPlayerVehicle != null && player.compensatedEntities.serverPlayerVehicle == vehicleID;
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

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            PacketEntity vehicle = player.compensatedEntities.getEntity(vehicleID);

            // Vanilla likes sending null vehicles, so we must ignore those like the client ignores them
            if (vehicle == null) return;

            // Eject existing passengers for this vehicle
            if (vehicle.passengers != null) {
                for (int entityID : vehicle.passengers) {
                    PacketEntity passenger = player.compensatedEntities.getEntity(entityID);
                    if (passenger == null) continue;

                    passenger.riding = null;
                }
            }

            // Add the entities as vehicles
            for (int entityID : passengers) {
                PacketEntity passenger = player.compensatedEntities.getEntity(entityID);
                if (passenger == null) continue;

                passenger.riding = vehicle;
            }

            vehicle.passengers = passengers;
        });
    }

    private void handleMoveEntity(PacketSendEvent event, int entityId, double deltaX, double deltaY, double deltaZ, Float yaw, Float pitch, boolean isRelative, boolean hasPos) {
        ServerPacketEntity data = player.compensatedEntities.serverEntityMap.get(entityId);

        if (!hasSentPreWavePacket) {
            hasSentPreWavePacket = true;
            player.sendTransaction();
        }

        if (data != null) {
            // Update the tracked server's entity position
            if (isRelative) {
                // ViaVersion sends two relative packets when moving more than 4 blocks
                // This is broken and causes the client to interpolate like (0, 4) and (1, 3) instead of (1, 7)
                // This causes impossible hits, so grim must replace this with a teleport entity packet
                // Not ideal, but neither is 1.8 players on a 1.9+ server.
                if ((Math.abs(deltaX) >= 3.9375 || Math.abs(deltaY) >= 3.9375 || Math.abs(deltaZ) >= 3.9375) && player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                    player.user.sendPacket(new WrapperPlayServerEntityTeleport(entityId, new Vector3d(data.getX() + deltaX, data.getY(), data.getZ()), yaw == null ? data.getXRot() : yaw, pitch == null ? data.getYRot() : pitch, false));
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
                xRotEntity.steps = xRotEntity.type == EntityTypes.BOAT ? 10 : 3;
            }
            entity.onFirstTransaction(isRelative, hasPos, deltaX, deltaY, deltaZ, player);
        });
        player.latencyUtils.addRealTimeTask(lastTrans + 1,() -> {
            PacketEntity entity = player.compensatedEntities.getEntity(entityId);
            if (entity == null) return;
            entity.onSecondTransaction();
        });
    }

    public void addEntity(User user, int entityID, EntityType type, Vector3d position, float xRot, float yRot, Integer data, List<EntityData> entityMetadata) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
        if (player == null) return;

        player.compensatedEntities.serverEntityMap.put(entityID, new ServerPacketEntity(
                position.getX(),
                position.getY(),
                position.getZ(),
                xRot,
                yRot,
                data,
                entityMetadata,
                player.lastTransactionSent.get()
        ));

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            player.compensatedEntities.addEntity(entityID, type, position, xRot);
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
        player.sendTransaction(); // We injected before vanilla flushes :) we don't need to flush
    }

    public void tickStartTick() {
        if (enablePreWavePacket) {
            hasSentPreWavePacket = false;
        }
    }
}
