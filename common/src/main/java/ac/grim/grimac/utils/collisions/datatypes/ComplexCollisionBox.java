package ac.grim.grimac.utils.collisions.datatypes;

import java.util.Arrays;
import java.util.List;

public class ComplexCollisionBox implements CollisionBox {

    // Most complex shape is the Modern MC Cauldron which is made up of 15 boxes
    public static int DEFAULT_MAX_COLLISION_BOX_SIZE = 15; // increase if we somehow have a shape made of more than 15 parts.
    private final SimpleCollisionBox[] boxes;
    private int currentLength;

    public ComplexCollisionBox(SimpleCollisionBox... boxes) {
        this(DEFAULT_MAX_COLLISION_BOX_SIZE, boxes);
    }

    public ComplexCollisionBox(int maxIndex) {
        this.boxes = new SimpleCollisionBox[maxIndex];
    }

    public ComplexCollisionBox(int maxIndex, SimpleCollisionBox... boxes) {
        this.boxes = new SimpleCollisionBox[maxIndex];
        currentLength = Math.min(maxIndex, boxes.length);
        System.arraycopy(boxes, 0, this.boxes, 0, this.currentLength);
    }

    public boolean add(SimpleCollisionBox collisionBox) {
        boxes[currentLength] = collisionBox;
        currentLength++;
        return currentLength <= boxes.length;
    }

    @Override
    public CollisionBox union(SimpleCollisionBox other) {
        add(other);
        return this;
    }

    @Override
    public boolean isCollided(SimpleCollisionBox other) {
        for (int i = 0; i < currentLength; i++) {
            if (boxes[i].isCollided(other)) return true;
        }
        return false;
    }

    @Override
    public boolean isIntersected(SimpleCollisionBox other) {
        for (int i = 0; i < currentLength; i++) {
            if (boxes[i].isIntersected(other)) return true;
        }
        return false;
    }

    @Override
    public CollisionBox copy() {
        ComplexCollisionBox copy = new ComplexCollisionBox(boxes.length);
        for (int i = 0; i < currentLength; i++) {
            copy.boxes[i] = boxes[i].copy();
        }
        copy.currentLength = this.currentLength;
        return copy;
    }

    @Override
    public CollisionBox offset(double x, double y, double z) {
        for (int i = 0; i < currentLength; i++) {
            boxes[i].offset(x, y ,z);
        }
        return this;
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        list.addAll(Arrays.asList(boxes).subList(0, currentLength));
    }

    @Override
    public int downCast(SimpleCollisionBox[] list) {
        System.arraycopy(boxes, 0, list, 0, currentLength);
        return currentLength;
    }

    @Override
    public boolean isNull() {
        for (int i = 0; i < currentLength; i++) {
            if (!boxes[i].isNull()) return false;
        }
        return true;
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }
}
