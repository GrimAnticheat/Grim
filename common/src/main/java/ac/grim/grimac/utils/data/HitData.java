package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.Vector3dm;

public class HitData {
    Vector3dm blockHitLocation;

    public HitData(Vector3dm blockHitLocation) {
        this.blockHitLocation = blockHitLocation;
    }

    public Vector3dm getBlockHitLocation() {
        return blockHitLocation;
    }
}
