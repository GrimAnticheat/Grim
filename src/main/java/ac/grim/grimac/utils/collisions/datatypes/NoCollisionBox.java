package ac.grim.grimac.utils.collisions.datatypes;

import java.util.List;

public class NoCollisionBox implements CollisionBox {

    public static final NoCollisionBox INSTANCE = new NoCollisionBox();

    private NoCollisionBox() {
    }

    @Override
    public boolean isCollided(SimpleCollisionBox other) {
        return false;
    }

    @Override
    public boolean isIntersected(SimpleCollisionBox other) {
        return false;
    }

    @Override
    public CollisionBox offset(double x, double y, double z) {
        return this;
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) { /**/ }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    @Override
    public CollisionBox copy() {
        return this;
    }
}