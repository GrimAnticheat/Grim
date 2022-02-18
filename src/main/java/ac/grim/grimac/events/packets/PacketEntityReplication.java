package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.utils.dependencies.viaversion.ViaVersionUtil;
import org.bukkit.entity.Entity;

import java.util.List;

public class PacketEntityReplication extends PacketCheck {

    private boolean hasSentPreWavePacket = false;

    public PacketEntityReplication(GrimPlayer player) {
        super(player);
    }

    public void tickFlying() {
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            entity.onMovement();
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
            addEntity(event.getUser(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), packetOutEntity.getEntityMetadata());
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packetOutEntity = new WrapperPlayServerSpawnEntity(event);
            addEntity(event.getUser(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), null);
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packetOutEntity = new WrapperPlayServerSpawnPlayer(event);
            addEntity(event.getUser(), packetOutEntity.getEntityId(), EntityTypes.PLAYER, packetOutEntity.getPosition(), packetOutEntity.getYaw(), packetOutEntity.getPitch(), packetOutEntity.getEntityMetadata());
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove move = new WrapperPlayServerEntityRelativeMove(event);
            handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), null, null, true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation move = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), move.getYaw() * 0.7111111F, move.getPitch() * 0.7111111F, true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport move = new WrapperPlayServerEntityTeleport(event);
            Vector3d pos = move.getPosition();
            handleMoveEntity(move.getEntityId(), pos.getX(), pos.getY(), pos.getZ(), move.getYaw(), move.getPitch(), false);
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);
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
            if (isDirectlyAffectingPlayer(player, entityID)) event.getPostTasks().add(player::sendTransaction);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1,
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

                event.setCancelled(true); // We replace this packet with an explosion packet
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
                player.compensatedEntities.serverPositionsMap.remove(entityID);
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

    private void handleMoveEntity(int entityId, double deltaX, double deltaY, double deltaZ, Float yaw, Float pitch, boolean isRelative) {
        PacketEntity reachEntity = player.compensatedEntities.getEntity(entityId);

        if (reachEntity != null) {
            // We can't hang two relative moves on one transaction
            if (reachEntity.lastTransactionHung == player.lastTransactionSent.get()) player.sendTransaction();
            reachEntity.lastTransactionHung = player.lastTransactionSent.get();

            // Only send one transaction before each wave, without flushing
            if (!hasSentPreWavePacket) player.sendTransaction();
            hasSentPreWavePacket = true; // Also functions to mark we need a post wave transaction

            TrackerData data = player.compensatedEntities.serverPositionsMap.get(entityId);

            if (data != null) {
                // Update the tracked server's entity position
                if (isRelative) {
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
            }

            int lastTrans = player.lastTransactionSent.get();

            player.latencyUtils.addRealTimeTask(lastTrans, () -> reachEntity.onFirstTransaction(isRelative, deltaX, deltaY, deltaZ, player));
            player.latencyUtils.addRealTimeTask(lastTrans + 1, reachEntity::onSecondTransaction);
        }
    }

    public void addEntity(User user, int entityID, EntityType type, Vector3d position, float xRot, float yRot, List<EntityData> entityMetadata) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
        if (player == null) return;

        player.compensatedEntities.serverPositionsMap.put(entityID, new TrackerData(position.getX(), position.getX(), position.getX(), xRot, yRot));

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            player.compensatedEntities.addEntity(entityID, type, position);
            if (entityMetadata != null) {
                player.compensatedEntities.updateEntityMetadata(entityID, entityMetadata);
            }
        });
    }

    private boolean isDirectlyAffectingPlayer(GrimPlayer player, int entityID) {
        if (player.bukkitPlayer == null) return false;
        Entity playerVehicle = player.bukkitPlayer.getVehicle();

        // The attributes for this entity is active, currently
        return (playerVehicle == null && entityID == player.entityID) ||
                (playerVehicle != null && entityID == playerVehicle.getEntityId());
    }

    public void onEndOfTickEvent() {
        // Only send a transaction at the end of the tick if we are tracking players
        player.sendTransaction(); // We injected before vanilla flushes :) we don't need to flush
        hasSentPreWavePacket = false;
    }
}
