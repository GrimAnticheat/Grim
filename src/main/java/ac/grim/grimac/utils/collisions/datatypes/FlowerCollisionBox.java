package ac.grim.grimac.utils.collisions.datatypes;

public class FlowerCollisionBox extends SimpleCollisionBox {
    public FlowerCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    @Override
    public SimpleCollisionBox offset(double x, double y, double z) {
        return super.offset(x, y, z);
    }
}
