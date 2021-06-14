package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.entity.Entity;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    private final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<SpawnEntityData> spawnEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Pair<Integer, int[]>> destroyEntityQueue = new ConcurrentLinkedQueue<>();
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

            addEntity(spawnEntity.entity, spawnEntity.position);
        }

        while (true) {
            EntityMoveData changeBlockData = moveEntityQueue.peek();
            if (changeBlockData == null) break;

            if (changeBlockData.lastTransactionSent > lastTransactionReceived) break;
            moveEntityQueue.poll();

            PacketEntity entity = getEntity(changeBlockData.entityID);

            // This is impossible without the server sending bad packets, but just to be safe...
            if (entity == null) continue;

            entity.position.add(new Vector3d(changeBlockData.deltaX, changeBlockData.deltaY, changeBlockData.deltaZ));
        }

        while (true) {
            EntityMetadataData data = importantMetadataQueue.peek();
            if (data == null) break;

            if (data.lastTransactionSent > lastTransactionReceived) break;
            importantMetadataQueue.poll();

            data.runnable.run();
        }

        while (true) {
            Pair<Integer, int[]> spawnEntity = destroyEntityQueue.peek();
            if (spawnEntity == null) break;

            if (spawnEntity.left() >= lastTransactionReceived) break;
            destroyEntityQueue.poll();

            for (int entityID : spawnEntity.right()) {
                entityMap.remove(entityID);
            }
        }
    }

    private void addEntity(Entity entity, Vector3d position) {
        PacketEntity packetEntity;

        // Uses strings instead of enum for version compatibility
        switch (entity.getType().name()) {
            case "Pig":
                packetEntity = new PacketEntityRideable(entity, position);
                break;
            case "Shulker":
                packetEntity = new PacketEntityShulker(entity, position);
                break;
            case "Strider":
                packetEntity = new PacketEntityStrider(entity, position);
                break;
            case "Donkey":
            case "Horse":
            case "Llama":
            case "Mule":
            case "SkeletonHorse":
            case "ZombieHorse":
            case "TraderLlama":
                packetEntity = new PacketEntityHorse(entity, position);
                break;
            default:
                packetEntity = new PacketEntity(entity, position);
        }

        entityMap.put(entity.getEntityId(), packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        return entityMap.get(entityID);
    }
}
