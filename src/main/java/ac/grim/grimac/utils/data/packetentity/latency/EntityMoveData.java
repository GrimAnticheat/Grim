package ac.grim.grimac.utils.data.packetentity.latency;

import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class EntityMoveData {
    int entityID;
    Vector3d relativeMove;
    int lastTransactionSent;

    public EntityMoveData(int entityID, double deltaX, double deltaY, double deltaZ, int lastTransactionSent) {
        this.entityID = entityID;
        this.relativeMove = relativeMove;
        this.lastTransactionSent = lastTransactionSent;
    }
}
