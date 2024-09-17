package ac.grim.grimac.utils.collisions.datatypes;

public class HexCollisionBox extends SimpleCollisionBox {

    // Mojang's block hitbox values are all based on chunks, so they're stored in game as 16 * the actual values we want
    // When copying block hitbox values, it may be easier to simple copy the multiplied values and divide them
    // HexCollisionBox is a simple extension of SimpleCollisionBox that does this for us.
    // If none of your min/max values are > 1 you probably should not be using this class.
    public HexCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX / 16d;
        this.minY = minY / 16d;
        this.minZ = minZ / 16d;
        this.maxX = maxX / 16d;
        this.maxY = maxY / 16d;
        this.maxZ = maxZ / 16d;
    }
}
