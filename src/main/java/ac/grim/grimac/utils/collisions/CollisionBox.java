package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;

import java.util.List;

public interface CollisionBox {
    boolean isCollided(CollisionBox other);

    boolean isIntersected(CollisionBox other);

    CollisionBox copy();

    CollisionBox offset(double x, double y, double z);

    void downCast(List<SimpleCollisionBox> list);

    boolean isNull();
}