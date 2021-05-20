package ac.grim.grimac.utils.collisions.datatypes;

import java.util.List;

public interface CollisionBox {
    boolean isCollided(CollisionBox other);

    boolean isIntersected(CollisionBox other);

    CollisionBox copy();

    CollisionBox offset(double x, double y, double z);

    void downCast(List<SimpleCollisionBox> list);

    boolean isNull();

    boolean isFullBlock();
}