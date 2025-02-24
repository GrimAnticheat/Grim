package ac.grim.grimac.utils.collisions.datatypes;

import com.github.retrooper.packetevents.protocol.world.BlockFace;

import java.util.List;

public interface CollisionBox {
    CollisionBox union(SimpleCollisionBox other);

    boolean isCollided(SimpleCollisionBox other);

    boolean isIntersected(SimpleCollisionBox other);

    CollisionBox copy();

    CollisionBox offset(double x, double y, double z);

    void downCast(List<SimpleCollisionBox> list);

    /**
     * @param list - A list that will contain all of the SimpleCollisionBoxes representing the CollisionBox.
     * @return - int representing the size of elements in the array that belong to this downcast.
     * The contents of elements starting from list[size] and above are undefined and should not be iterated over.
     */
    int downCast(SimpleCollisionBox[] list);

    boolean isNull();

    boolean isFullBlock();

    default boolean isSideFullBlock(BlockFace axis) {
        return isFullBlock();
    }
}
