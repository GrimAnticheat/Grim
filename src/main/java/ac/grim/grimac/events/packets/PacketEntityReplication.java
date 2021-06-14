package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
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

            Entity entity = entityMetadata.getEntity();

            if (entity instanceof Shulker) {
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

            switch (entity.getType()) {
                case PIG:
                    ((Pig) entity).hasSaddle();
                    break;
            }
        }
    }
}
