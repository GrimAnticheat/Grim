package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.entity.Entity;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    private final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<SpawnEntityData> spawnEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<ChangeBlockData> importantMetadataQueue = new ConcurrentLinkedQueue<>();


    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void addGenericEntity(Entity entity, Vector3d position) {
        entityMap.put(entity.getEntityId(), new PacketEntity(entity));
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
