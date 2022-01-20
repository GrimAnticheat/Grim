package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.utils.dependencies.viaversion.ViaVersionUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
            addEntity((Player) event.getPlayer(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition());
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packetOutEntity = new WrapperPlayServerSpawnEntity(event);
            addEntity((Player) event.getPlayer(), packetOutEntity.getEntityId(), packetOutEntity.getEntityType(), packetOutEntity.getPosition());
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packetOutEntity = new WrapperPlayServerSpawnPlayer(event);
            addEntity((Player) event.getPlayer(), packetOutEntity.getEntityId(), EntityTypes.PLAYER, packetOutEntity.getPosition());
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove move = new WrapperPlayServerEntityRelativeMove(event);
            handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation move = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), true);
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport move = new WrapperPlayServerEntityTeleport(event);
            Vector3d pos = move.getPosition();
            handleMoveEntity(move.getEntityId(), pos.getX(), pos.getY(), pos.getZ(), false);
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.updateEntityMetadata(entityMetadata.getEntityId(), entityMetadata.getEntityMetadata()));
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect effect = new WrapperPlayServerEntityEffect(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
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

            if (isDirectlyAffectingPlayer(player, effect.getEntityId())) event.setPostTask(player::sendTransaction);

            player.compensatedPotions.addPotionEffect(type, effect.getEffectAmplifier(), effect.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrapperPlayServerRemoveEntityEffect effect = new WrapperPlayServerRemoveEntityEffect(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            if (isDirectlyAffectingPlayer(player, effect.getEntityId())) event.setPostTask(player::sendTransaction);

            player.compensatedPotions.removePotionEffect(effect.getPotionType(), effect.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_PROPERTIES) {
            WrapperPlayServerEntityProperties attributes = new WrapperPlayServerEntityProperties(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            int entityID = attributes.getEntityId();

            PacketEntity entity = player.compensatedEntities.getEntity(attributes.getEntityId());

            // The attributes for this entity is active, currently
            if (isDirectlyAffectingPlayer(player, entityID)) event.setPostTask(player::sendTransaction);

            if (player.entityID == entityID || entity instanceof PacketEntityHorse || entity instanceof PacketEntityRideable) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1,
                        () -> player.compensatedEntities.updateAttributes(entityID, attributes.getProperties()));
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
            WrapperPlayServerEntityStatus status = new WrapperPlayServerEntityStatus(event);
            // This hasn't changed from 1.7.2 to 1.17
            // Needed to exempt players on dead vehicles, as dead entities have strange physics.
            if (status.getStatus() == 3) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
                if (player == null) return;

                PacketEntity entity = player.compensatedEntities.getEntity(status.getEntityId());

                if (entity == null) return;
                entity.isDead = true;
            }

            if (status.getStatus() == 9) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
                if (player == null) return;

                if (status.getEntityId() != player.entityID) return;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            if (slot.getWindowId() == 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (slot.getSlot() - 36 == player.packetStateData.lastSlotSelected) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                    }
                });

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                    if (slot.getSlot() - 36 == player.packetStateData.lastSlotSelected) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                    }
                });
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers mount = new WrapperPlayServerSetPassengers(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            int vehicleID = mount.getEntityId();
            int[] passengers = mount.getPassengers();

            handleMountVehicle(vehicleID, passengers);
        }

        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            WrapperPlayServerAttachEntity attach = new WrapperPlayServerAttachEntity(event);

            // This packet was replaced by the mount packet on 1.9+ servers - to support multiple passengers on one vehicle
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            // If this is mounting rather than leashing
            if (!attach.isLeash()) {
                handleMountVehicle(attach.getHoldingId(), new int[]{attach.getAttachedId()});
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            int[] destroyEntityIds = destroy.getEntityIds();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                for (int integer : destroyEntityIds) {
                    player.compensatedEntities.entityMap.remove(integer);
                    player.compensatedFireworks.removeFirework(integer);
                    player.compensatedPotions.removeEntity(integer);
                }
            });
        }
    }

    private void handleMountVehicle(int vehicleID, int[] passengers) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            PacketEntity vehicle = player.compensatedEntities.getEntity(vehicleID);

            // Eject existing passengers for this vehicle
            if (vehicle.passengers != null) {
                for (int entityID : vehicle.passengers) {
                    PacketEntity passenger = player.compensatedEntities.getEntity(entityID);

                    if (passenger == null)
                        continue;

                    passenger.riding = null;
                }
            }

            // Add the entities as vehicles
            for (int entityID : passengers) {
                PacketEntity passenger = player.compensatedEntities.getEntity(entityID);
                if (passenger == null)
                    continue;

                passenger.riding = vehicle;
            }

            vehicle.passengers = passengers;
        });
    }

    private void handleMoveEntity(int entityId, double deltaX, double deltaY, double deltaZ, boolean isRelative) {
        PacketEntity reachEntity = player.compensatedEntities.getEntity(entityId);

        if (reachEntity != null) {
            // We can't hang two relative moves on one transaction
            if (reachEntity.lastTransactionHung == player.lastTransactionSent.get()) player.sendTransaction();
            reachEntity.lastTransactionHung = player.lastTransactionSent.get();

            // Only send one transaction before each wave, without flushing
            if (!hasSentPreWavePacket) player.sendTransaction();
            hasSentPreWavePacket = true; // Also functions to mark we need a post wave transaction

            // Update the tracked server's entity position
            if (isRelative)
                reachEntity.serverPos = reachEntity.serverPos.add(new Vector3d(deltaX, deltaY, deltaZ));
            else
                reachEntity.serverPos = new Vector3d(deltaX, deltaY, deltaZ);

            int lastTrans = player.lastTransactionSent.get();
            Vector3d newPos = reachEntity.serverPos;

            player.latencyUtils.addRealTimeTask(lastTrans, () -> reachEntity.onFirstTransaction(newPos.getX(), newPos.getY(), newPos.getZ(), player));
            player.latencyUtils.addRealTimeTask(lastTrans + 1, reachEntity::onSecondTransaction);
        }
    }

    public void addEntity(Player bukkitPlayer, int entityID, EntityType type, Vector3d position) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(bukkitPlayer);
        if (player == null) return;

        player.compensatedEntities.addEntity(entityID, type, position);
    }

    private boolean isDirectlyAffectingPlayer(GrimPlayer player, int entityID) {
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
