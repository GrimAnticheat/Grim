package ac.grim.grimac.utils.collisions.datatypes;

import com.github.retrooper.packetevents.protocol.world.BlockFace;

import java.util.List;

public interface CollisionBox {
    boolean isCollided(SimpleCollisionBox other);

    boolean isIntersected(SimpleCollisionBox other);

    CollisionBox copy();

    CollisionBox offset(double x, double y, double z);

    void downCast(List<SimpleCollisionBox> list);

    boolean isNull();

    boolean isFullBlock();

    default boolean isSideFullBlock(BlockFace axis) {
        return isFullBlock();
    }
}