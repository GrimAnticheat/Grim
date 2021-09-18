package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMountData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityPropertiesData;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
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
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentity.WrappedPacketOutSpawnEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import io.github.retrooper.packetevents.packetwrappers.play.out.updateattributes.WrappedPacketOutUpdateAttributes;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.versionlookup.viaversion.ViaVersionLookupUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

public class PacketEntityReplication extends PacketListenerAbstract {

    public PacketEntityReplication() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN || packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving packetOutEntity = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            EntityType type = EntityType.ZOMBIE; // Fall back to zombie type
            // Try a second time
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(packetOutEntity.getEntityId());
            // Final attempt to get this entity, otherwise it likely doesn't exist
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(packetOutEntity.getEntityId());

            if (entity != null) {
                type = entity.getType();
            }

            player.compensatedEntities.addEntity(packetOutEntity.getEntityId(), type, packetOutEntity.getPosition());
        }

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutSpawnEntity packetOutEntity = new WrappedPacketOutSpawnEntity(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            EntityType type = EntityType.ZOMBIE; // Fall back to zombie type
            // Try a second time
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(packetOutEntity.getEntityId());
            // Final attempt to get this entity, otherwise it likely doesn't exist
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(packetOutEntity.getEntityId());

            if (entity != null) {
                type = entity.getType();
            }

            player.compensatedEntities.addEntity(packetOutEntity.getEntityId(), type, packetOutEntity.getPosition());
        }

        if (packetID == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn spawn = new WrappedPacketOutNamedEntitySpawn(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            Entity entity = spawn.getEntity();
            EntityType type = EntityType.ZOMBIE; // Fall back to zombie type
            // Try a second time
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(spawn.getEntityId());
            // Final attempt to get this entity, otherwise it likely doesn't exist
            if (entity == null)
                PacketEvents.get().getServerUtils().getEntityById(spawn.getEntityId());

            if (entity != null) {
                type = entity.getType();
            }

            player.compensatedEntities.addEntity(spawn.getEntityId(), type, spawn.getPosition());
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            if (move.getDeltaX() != 0 || move.getDeltaY() != 0 || move.getDeltaZ() != 0) {
                player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(move.getEntityId(),
                        move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), player.lastTransactionSent.get(), true));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport teleport = new WrappedPacketOutEntityTeleport(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            Vector3d position = teleport.getPosition();

            player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(teleport.getEntityId(),
                    position.getX(), position.getY(), position.getZ(), player.lastTransactionSent.get(), false));
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), entityMetadata.getWatchableObjects(), player.lastTransactionSent.get()));
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

            event.setPostTask(player::sendTransaction);
            player.compensatedPotions.addPotionEffect(type.getName(), effect.getAmplifier(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            WrappedPacketOutRemoveEntityEffect effect = new WrappedPacketOutRemoveEntityEffect(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            event.setPostTask(player::sendTransaction);
            player.compensatedPotions.removePotionEffect(PotionEffectType.getById(effect.getEffectId()).getName(), effect.getEntityId());
        }

        if (packetID == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrappedPacketOutUpdateAttributes attributes = new WrappedPacketOutUpdateAttributes(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int entityID = attributes.getEntityId();

            PacketEntity entity = player.compensatedEntities.getEntity(attributes.getEntityId());
            Entity playerVehicle = player.bukkitPlayer.getVehicle();

            // The attributes for this entity is active, currently
            if ((playerVehicle == null && entityID == player.entityID) ||
                    (playerVehicle != null && entityID == playerVehicle.getEntityId())) {
                event.setPostTask(player::sendTransaction);
            }

            if (player.entityID == entityID || entity instanceof PacketEntityHorse || entity instanceof PacketEntityRideable) {
                player.compensatedEntities.entityPropertiesData.add(new EntityPropertiesData(entityID, attributes.getProperties(), player.lastTransactionSent.get() + 1));
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
                event.setPostTask(player::sendTransaction);
            }
        }

        if (packetID == PacketType.Play.Server.MOUNT) {
            WrappedPacketOutMount mount = new WrappedPacketOutMount(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int vehicleID = mount.getEntityId();
            int[] passengers = mount.getPassengerIds();

            player.compensatedEntities.mountVehicleQueue.add(new EntityMountData(vehicleID, passengers, player.lastTransactionSent.get()));
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

                player.compensatedEntities.mountVehicleQueue.add(new EntityMountData(vehicleID, passengers, player.lastTransactionSent.get()));
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            int[] destroyEntityIds = destroy.getEntityIds();

            for (int integer : destroyEntityIds) {
                PacketEntity entity = player.compensatedEntities.getEntity(integer);
                if (entity == null) continue;
                entity.setDestroyed(lastTransactionSent + 1);
            }
        }
    }
}
