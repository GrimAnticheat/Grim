package ac.grim.grimac.utils.collisions.types;

public class HexCollisionBox extends SimpleCollisionBox {
    public HexCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX / 16;
        this.minY = minY / 16;
        this.minZ = minZ / 16;
        this.maxX = maxX / 16;
        this.maxY = maxY / 16;
        this.maxZ = maxZ / 16;
    }
}
