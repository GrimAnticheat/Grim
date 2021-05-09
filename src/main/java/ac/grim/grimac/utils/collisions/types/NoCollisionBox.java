package ac.grim.grimac.utils.collisions.types;

import ac.grim.grimac.utils.collisions.CollisionBox;

import java.util.List;

public class NoCollisionBox implements CollisionBox {

    public static final NoCollisionBox INSTANCE = new NoCollisionBox();

    private NoCollisionBox() {
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        return false;
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
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