package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityeffect.WrappedPacketOutEntityEffect;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.mount.WrappedPacketOutMount;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import io.github.retrooper.packetevents.packetwrappers.play.out.removeentityeffect.WrappedPacketOutRemoveEntityEffect;
import io.github.retrooper.packetevents.packetwrappers.play.out.setslot.WrappedPacketOutSetSlot;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentity.WrappedPacketOutSpawnEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import io.github.retrooper.packetevents.packetwrappers.play.out.updateattributes.WrappedPacketOutUpdateAttributes;
import io.github.retrooper.packetevents.packetwrappers.play.out.windowitems.WrappedPacketOutWindowItems;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.versionlookup.viaversion.ViaVersionLookupUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

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
    public void onPacketReceive(PacketPlayReceiveEvent event) {
        if (PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId())) {
            // Teleports don't interpolate, duplicate 1.17 packets don't interpolate
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;
            tickFlying();
        }
    }

    @Override
    public void onPacketSend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN || packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving packetOutEntity = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());
            addEntity(event.getPlayer(), packetOutEntity.getEntityId(), packetOutEntity.getPosition());
        }
        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutSpawnEntity packetOutEntity = new WrappedPacketOutSpawnEntity(event.getNMSPacket());
            addEntity(event.getPlayer(), packetOutEntity.getEntityId(), packetOutEntity.getPosition());
        }
        if (packetID == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn packetOutEntity = new WrappedPacketOutNamedEntitySpawn(event.getNMSPacket());
            addEntity(event.getPlayer(), packetOutEntity.getEntityId(), packetOutEntity.getPosition());
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());
            handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), true);
        }
        if (packetID == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport move = new WrappedPacketOutEntityTeleport(event.getNMSPacket());
            Vector3d pos = move.getPosition();
            handleMoveEntity(move.getEntityId(), pos.getX(), pos.getY(), pos.getZ(), false);
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());
            player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.updateEntityMetadata(entityMetadata.getEntityId(), entityMetadata.getWatchableObjects()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_EFFECT) {
            WrappedPacketOutEntityEffect effect = new WrappedPacketOutEntityEffect(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            PotionEffectType type = PotionEffectType.getById(effect.getEffectId());

            // ViaVersion tries faking levitation effects and fails badly lol, flagging the anticheat
            // Block other effects just in case ViaVersion gets any ideas
            //
            // Set to 24 so ViaVersion blocks it
            // 24 is the levitation effect
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_9) && ViaVersionLookupUtils.isAvailable() && effect.getEffectId() > 23) {
                event.setCancelled(true);
                return;
            }

            // ViaVersion dolphin's grace also messes us up, set it to a potion effect that doesn't exist on 1.12
            // Effect 31 is bad omen
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_13) && ViaVersionLookupUtils.isAvailable() && effect.getEffectId() == 30) {
                event.setCancelled(true);
                return;
            }

            if (isDirectlyAffectingPlayer(player, effect.getEntityId())) event.setPostTask(player::sendTransaction);

            player.compensatedPotions.addPotionEffect(type.getName(), effect.getAmplifier(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrappedPacketOutRemoveEntityEffect effect = new WrappedPacketOutRemoveEntityEffect(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            if (isDirectlyAffectingPlayer(player, effect.getEntityId())) event.setPostTask(player::sendTransaction);

            player.compensatedPotions.removePotionEffect(PotionEffectType.getById(effect.getEffectId()).getName(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrappedPacketOutUpdateAttributes attributes = new WrappedPacketOutUpdateAttributes(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int entityID = attributes.getEntityId();

            PacketEntity entity = player.compensatedEntities.getEntity(attributes.getEntityId());

            // The attributes for this entity is active, currently
            if (isDirectlyAffectingPlayer(player, entityID)) event.setPostTask(player::sendTransaction);

            if (player.entityID == entityID || entity instanceof PacketEntityHorse || entity instanceof PacketEntityRideable) {
                player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get() + 1,
                        () -> player.compensatedEntities.updateAttributes(entityID, attributes.getProperties()));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_STATUS) {
            WrappedPacketOutEntityStatus status = new WrappedPacketOutEntityStatus(event.getNMSPacket());
            // This hasn't changed from 1.7.2 to 1.17
            // Needed to exempt players on dead vehicles, as dead entities have strange physics.
            if (status.getEntityStatus() == 3) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
                if (player == null) return;

                PacketEntity entity = player.compensatedEntities.getEntity(status.getEntityId());

                if (entity == null) return;
                entity.isDead = true;
            }

            if (status.getEntityStatus() == 9) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
                if (player == null) return;

                if (status.getEntityId() != player.entityID) return;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
            }
        }

        if (packetID == PacketType.Play.Server.SET_SLOT) {
            WrappedPacketOutSetSlot slot = new WrappedPacketOutSetSlot(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
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

        if (packetID == PacketType.Play.Server.WINDOW_ITEMS) {
            WrappedPacketOutWindowItems items = new WrappedPacketOutWindowItems(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE);
            }
        }

        if (packetID == PacketType.Play.Server.MOUNT) {
            WrappedPacketOutMount mount = new WrappedPacketOutMount(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int vehicleID = mount.getEntityId();
            int[] passengers = mount.getPassengerIds();

            handleMountVehicle(vehicleID, passengers);
        }

        if (packetID == PacketType.Play.Server.ATTACH_ENTITY) {
            WrappedPacket attach = new WrappedPacket(event.getNMSPacket());

            // This packet was replaced by the mount packet on 1.9+ servers - to support multiple passengers on one vehicle
            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) return;

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // If this is mounting rather than leashing
            if (attach.readInt(0) == 0) {
                int vehicleID = attach.readInt(2);
                int[] passengers = new int[]{attach.readInt(1)};

                handleMountVehicle(vehicleID, passengers);
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int[] destroyEntityIds = destroy.getEntityIds();

            for (int integer : destroyEntityIds) {
                player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.entityMap.remove(integer));
            }
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

    public void addEntity(Player bukkitPlayer, int entityID, Vector3d position) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(bukkitPlayer);
        if (player == null) return;

        EntityType type = EntityType.ZOMBIE; // Fall back to zombie type
        Entity entity = PacketEvents.get().getServerUtils().getEntityById(entityID);

        // Try a second time
        if (entity == null)
            entity = PacketEvents.get().getServerUtils().getEntityById(entityID);
        // Try a third time
        if (entity == null)
            entity = PacketEvents.get().getServerUtils().getEntityById(entityID);

        if (entity != null) {
            type = entity.getType();
        }

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
