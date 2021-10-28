package ac.grim.grimac.utils.data.packetentity.latency;

import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeSnapshotWrapper;

import java.util.List;

public class EntityPropertiesData {
    public final int entityID;
    public final List<AttributeSnapshotWrapper> objects;

    public EntityPropertiesData(int entityID, List<AttributeSnapshotWrapper> objects, int lastTransactionSent) {
        this.entityID = entityID;
        this.objects = objects;
    }
}
