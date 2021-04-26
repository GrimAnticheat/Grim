package ac.grim.grimac.utils.collisions.types;

public class HexCollisionBox extends SimpleCollisionBox {
    public HexCollisionBox() {
        this(0, 0, 0, 0, 0, 0);
    }

    public HexCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX < maxX) {
            this.minX = minX / 16;
            this.maxX = maxX / 16;
        } else {
            this.minX = maxX / 16;
            this.maxX = minX / 16;
        }
        if (minY < maxY) {
            this.minY = minY / 16;
            this.maxY = maxY / 16;
        } else {
            this.minY = maxY / 16;
            this.maxY = minY / 16;
        }
        if (minZ < maxZ) {
            this.minZ = minZ / 16;
            this.maxZ = maxZ / 16;
        } else {
            this.minZ = maxZ / 16;
            this.maxZ = minZ / 16;
        }
    }
}
