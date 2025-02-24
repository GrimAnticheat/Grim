package ac.grim.grimac.utils.collisions.datatypes;

public class HexCollisionBox extends SimpleCollisionBox {

    /**
     * Creates a box defined by two points in 3d space; used to represent hitboxes and collision boxes.
     * Mojang's block hitbox values are all based on chunks, so they're stored in game as 16 * the actual size
     * When copying block hitbox values, it may be easier to simple copy the multiplied values and use this class
     * If your min/max values are < 1 you should probably check out {@link SimpleCollisionBox}
     * @param minX 16 * x position of first corner
     * @param minY 16 * y position of first corner
     * @param minZ 16 * z position of first corner
     * @param maxX 16 * x position of second corner
     * @param maxY 16 * y position of second corner
     * @param maxZ 16 * z position of second corner
     */
    public HexCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX / 16d;
        this.minY = minY / 16d;
        this.minZ = minZ / 16d;
        this.maxX = maxX / 16d;
        this.maxY = maxY / 16d;
        this.maxZ = maxZ / 16d;
    }
}
