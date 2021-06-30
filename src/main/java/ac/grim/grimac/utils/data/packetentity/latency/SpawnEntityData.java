package ac.grim.grimac.utils.data.packetentity.latency;

import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class SpawnEntityData {
    public final int entity;
    public Vector3d position;
    public int lastTransactionSent;

    public SpawnEntityData(int entity, Vector3d position, int lastTransactionSent) {
        this.entity = entity;
        this.position = position;
        this.lastTransactionSent = lastTransactionSent;
    }
}
