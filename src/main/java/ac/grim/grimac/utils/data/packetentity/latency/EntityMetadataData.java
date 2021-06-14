package ac.grim.grimac.utils.data.packetentity.latency;

import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;

import java.util.List;

public class EntityMetadataData {
    public final int entityID;
    public final List<WrappedWatchableObject> objects;
    public int lastTransactionSent;

    public EntityMetadataData(int entityID, List<WrappedWatchableObject> objects, int lastTransactionSent) {
        this.entityID = entityID;
        this.objects = objects;
        this.lastTransactionSent = lastTransactionSent;
    }
}
