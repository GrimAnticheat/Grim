package ac.grim.grimac.utils.collisions.datatypes;

public class HexCollisionBox extends SimpleCollisionBox {
    public HexCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX / 16d;
        this.minY = minY / 16d;
        this.minZ = minZ / 16d;
        this.maxX = maxX / 16d;
        this.maxY = maxY / 16d;
        this.maxZ = maxZ / 16d;
    }
}
