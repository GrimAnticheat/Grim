package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class ReachMovementData {
    public int transactionID;
    public int entityID;
    public Vector3d newPos;

    public ReachMovementData(int transactionID, int entityID, Vector3d newPos) {
        this.transactionID = transactionID;
        this.entityID = entityID;
        this.newPos = newPos;
    }
}
