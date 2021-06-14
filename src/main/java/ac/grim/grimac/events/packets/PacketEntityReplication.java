package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Optional;

public class PacketEntityReplication extends PacketListenerAbstract {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY || packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN
                || packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving packetOutEntity = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            if (entity == null) return;

            player.compensatedEntities.spawnEntityQueue.add(new SpawnEntityData(entity, packetOutEntity.getPosition(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            int[] destroyEntityIds = destroy.getEntityIds();

            player.compensatedEntities.destroyEntityQueue.add(new Pair<Integer, int[]>() {
                @Override
                public Integer left() {
                    return lastTransactionSent;
                }

                @Override
                public int[] right() {
                    return destroyEntityIds;
                }
            });
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(move.getEntityId(),
                    move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            PacketEntity entity = player.compensatedEntities.getEntity(entityMetadata.getEntityId());

            Optional<WrappedWatchableObject> poseObject = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 6).findFirst();
            if (poseObject.isPresent()) {
                Pose pose = Pose.valueOf(poseObject.get().getRawValue().toString().toUpperCase());

                Bukkit.broadcastMessage("Pose is " + pose);
                player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () ->
                        entity.pose = pose, player.lastTransactionSent.get()));
            }

            if (entity instanceof PacketEntityShulker) {
                Optional<WrappedWatchableObject> shulkerAttached = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 15).findFirst();
                if (shulkerAttached.isPresent()) {
                    // This NMS -> Bukkit conversion is great and works in all 11 versions.
                    BlockFace face = BlockFace.valueOf(shulkerAttached.get().getRawValue().toString().toUpperCase());

                    Bukkit.broadcastMessage("Shulker blockface is " + face);
                    player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(),
                            () -> ((PacketEntityShulker) entity).facing = face, player.lastTransactionSent.get()));
                }

                Optional<WrappedWatchableObject> height = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                if (height.isPresent()) {
                    Bukkit.broadcastMessage("Shulker has opened it's shell! " + height.get().getRawValue());
                    player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () -> {
                        ((PacketEntityShulker) entity).wantedShieldHeight = (byte) height.get().getRawValue();
                        ((PacketEntityShulker) entity).lastShieldChange = System.currentTimeMillis();
                    }, player.lastTransactionSent.get()));
                }
            }

            if (entity instanceof PacketEntityRideable) {
                if (entity.entity.getType() == EntityType.PIG) {
                    Optional<WrappedWatchableObject> pigSaddle = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                    if (pigSaddle.isPresent()) {
                        // Set saddle code
                        Bukkit.broadcastMessage("Pig saddled " + pigSaddle.get().getRawValue());
                        player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(),
                                () -> ((PacketEntityRideable) entity).hasSaddle = (boolean) pigSaddle.get().getRawValue(), player.lastTransactionSent.get()));
                    }

                    Optional<WrappedWatchableObject> pigBoost = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                    if (pigBoost.isPresent()) {
                        // Set pig boost code
                        Bukkit.broadcastMessage("Pig boost " + pigBoost.get().getRawValue());
                        player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () -> {
                            ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.get().getRawValue();
                            ((PacketEntityRideable) entity).currentBoostTime = 0;
                        }, player.lastTransactionSent.get()));
                    }
                } else if (entity instanceof PacketEntityStrider) { // Strider
                    Optional<WrappedWatchableObject> striderBoost = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                    if (striderBoost.isPresent()) {
                        // Set strider boost code
                        Bukkit.broadcastMessage("Strider boost " + striderBoost.get().getRawValue());
                        player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () -> {
                            ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.get().getRawValue();
                            ((PacketEntityRideable) entity).currentBoostTime = 0;
                        }, player.lastTransactionSent.get()));
                    }

                    Optional<WrappedWatchableObject> striderShaking = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                    if (striderShaking.isPresent()) {
                        // Set strider shaking code
                        Bukkit.broadcastMessage("Strider shaking " + striderShaking.get().getRawValue());
                        player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(),
                                () -> ((PacketEntityStrider) entity).isShaking = (boolean) striderShaking.get().getRawValue(), player.lastTransactionSent.get()));
                    }

                    Optional<WrappedWatchableObject> striderSaddle = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 18).findFirst();
                    if (striderSaddle.isPresent()) {
                        // Set saddle code
                        Bukkit.broadcastMessage("Strider saddled " + striderSaddle.get().getRawValue());
                        player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () -> {
                            ((PacketEntityRideable) entity).hasSaddle = (boolean) striderSaddle.get().getRawValue();
                        }, player.lastTransactionSent.get()));
                    }
                }
            }

            if (entity instanceof PacketEntityHorse) {
                Optional<WrappedWatchableObject> horseByte = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                if (horseByte.isPresent()) {
                    byte info = (byte) horseByte.get().getRawValue();

                    Bukkit.broadcastMessage("Horse " + (info & 0x04) + " " + (info & 0x20));
                    player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), () -> {
                        ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                        ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
                    }, player.lastTransactionSent.get()));
                }
            }
        }
    }
}
