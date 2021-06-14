package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.entity.Entity;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    private final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<SpawnEntityData> spawnEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Pair<Integer, Integer>> destroyEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMetadataData> importantMetadataQueue = new ConcurrentLinkedQueue<>();


    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void tickUpdates(int lastTransactionReceived) {
        while (true) {
            SpawnEntityData spawnEntity = spawnEntityQueue.peek();

            if (spawnEntity == null) break;

            if (spawnEntity.lastTransactionSent >= lastTransactionReceived) break;
            spawnEntityQueue.poll();

            player.compensatedEntities.addGenericEntity(spawnEntity.entity, spawnEntity.position);
        }

        while (true) {
            EntityMoveData changeBlockData = moveEntityQueue.peek();

            if (changeBlockData == null) break;
            // The player hasn't gotten this update yet
            if (changeBlockData.lastTransactionSent > lastTransactionReceived) {
                break;
            }

            moveEntityQueue.poll();

            PacketEntity entity = player.compensatedEntities.getEntity(changeBlockData.entityID);

            // This is impossible without the server sending bad packets
            if (entity == null)
                continue;

            entity.position.add(new Vector3d(changeBlockData.deltaX, changeBlockData.deltaY, changeBlockData.deltaZ));
        }

        while (true) {
            EntityMetadataData data = importantMetadataQueue.peek();

            if (data == null) break;

            // The player hasn't gotten this update yet
            if (data.lastTransactionSent > lastTransactionReceived) {
                break;
            }

            importantMetadataQueue.poll();

            PacketEntity entity = player.compensatedEntities.getEntity(data.entityID);

            // This is impossible without the server sending bad packets
            if (entity == null)
                continue;

            data.runnable.run();
        }
    }

    public void addGenericEntity(Entity entity, Vector3d position) {
        entityMap.put(entity.getEntityId(), new PacketEntity(entity, position));
    }

    public PacketEntity getEntity(int entityID) {
        return entityMap.get(entityID);
    }

    public void removeEntity(int[] removedEntities) {
        for (int i : removedEntities) {
            entityMap.remove(i);
        }
    }
}
