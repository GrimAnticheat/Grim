package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityShulker;
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
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Shulker;

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

            int[] destroyEntityIds = destroy.getEntityIds();

            player.compensatedEntities.removeEntity(destroyEntityIds);
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
            }

            if (entity instanceof PacketEntityShulker) {
                Optional<WrappedWatchableObject> shulkerAttached = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 15).findFirst();
                if (shulkerAttached.isPresent()) {
                    // This NMS -> Bukkit conversion is great and works in all 11 versions.
                    BlockFace face = BlockFace.valueOf(shulkerAttached.get().getRawValue().toString().toUpperCase());

                    Bukkit.broadcastMessage("Shulker blockface is " + face);
                }

                Optional<WrappedWatchableObject> height = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                if (height.isPresent()) {
                    Bukkit.broadcastMessage("Shulker has opened it's shell! " + height.get().getRawValue());
                }
            }

            if (entity instanceof PacketEntityRideable) {
                if (entity.entity.getType() == EntityType.PIG) {
                    Optional<WrappedWatchableObject> pigSaddle = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                    if (pigSaddle.isPresent()) {
                        // Set saddle code
                        Bukkit.broadcastMessage("Pig saddled " + pigSaddle.get().getRawValue());
                    }

                    Optional<WrappedWatchableObject> pigBoost = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                    if (pigBoost.isPresent()) {
                        // Set pig boost code
                        Bukkit.broadcastMessage("Pig boost " + pigBoost.get().getRawValue());
                    }
                } else { // Strider
                    Optional<WrappedWatchableObject> striderBoost = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                    if (striderBoost.isPresent()) {
                        // Set strider boost code
                        Bukkit.broadcastMessage("Strider boost " + striderBoost.get().getRawValue());
                    }

                    Optional<WrappedWatchableObject> striderShaking = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 17).findFirst();
                    if (striderShaking.isPresent()) {
                        // Set strider shaking code
                        Bukkit.broadcastMessage("Strider shaking " + striderShaking.get().getRawValue());
                    }

                    Optional<WrappedWatchableObject> striderSaddle = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 18).findFirst();
                    if (striderSaddle.isPresent()) {
                        // Set saddle code
                        Bukkit.broadcastMessage("Strider saddle " + striderSaddle.get().getRawValue());
                    }
                }
            }

            if (entity instanceof PacketEntityHorse) {
                Optional<WrappedWatchableObject> horseByte = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == 16).findFirst();
                if (horseByte.isPresent()) {
                    byte info = (byte) horseByte.get().getRawValue();

                    // Saddle
                    if ((info & 0x04) != 0) {
                        Bukkit.broadcastMessage("Horse saddled " + (info & 0x04));
                    }

                    // Rearing
                    if ((info & 0x20) != 0) {
                        Bukkit.broadcastMessage("Pig rearing " + (info & 0x20));
                    }
                }
            }
        }
    }
}
