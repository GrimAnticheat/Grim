package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PlayerReachEntity {
    public SimpleCollisionBox targetLocation;
    public SimpleCollisionBox currentLocation;

    public Vector3d interpAmount;
    public int interpSteps;

    public Vector3d relativeMoveLocation;
    public Vector3d serverPos;

    public PlayerReachEntity(double x, double y, double z) {
        this.currentLocation = GetBoundingBox.getBoundingBoxFromPosAndSize(x, y, z, 0.6, 1.8);
        this.targetLocation = currentLocation.copy();

        relativeMoveLocation = new Vector3d(x, y, z);
        serverPos = new Vector3d(x, y, z);
    }
}
