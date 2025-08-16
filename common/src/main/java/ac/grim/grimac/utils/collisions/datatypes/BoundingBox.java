package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.utils.math.Vector3dm;

public class BoundingBox {

    public final float minX, minY, minZ, maxX, maxY, maxZ;

    public BoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BoundingBox(Vector3dm min, Vector3dm max) {
        this.minX = (float) Math.min(min.getX(), max.getX());
        this.minY = (float) Math.min(min.getY(), max.getY());
        this.minZ = (float) Math.min(min.getZ(), max.getZ());
        this.maxX = (float) Math.max(min.getX(), max.getX());
        this.maxY = (float) Math.max(min.getY(), max.getY());
        this.maxZ = (float) Math.max(min.getZ(), max.getZ());
    }

    public BoundingBox(BoundingBox one, BoundingBox two) {
        this.minX = Math.min(one.minX, two.minX);
        this.minY = Math.min(one.minY, two.minY);
        this.minZ = Math.min(one.minZ, two.minZ);
        this.maxX = Math.max(one.maxX, two.maxX);
        this.maxY = Math.max(one.maxY, two.maxY);
        this.maxZ = Math.max(one.maxZ, two.maxZ);
    }

    public BoundingBox add(float x, float y, float z) {
        return new BoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public BoundingBox add(Vector3dm vector) {
        return add((float) vector.getX(), (float) vector.getY(), (float) vector.getZ());
    }

    public BoundingBox grow(float x, float y, float z) {
        return new BoundingBox(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    public BoundingBox shrink(float x, float y, float z) {
        return new BoundingBox(minX + x, minY + y, minZ + z, maxX - x, maxY - y, maxZ - z);
    }

    public BoundingBox add(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new BoundingBox(this.minX + minX, this.minY + minY, this.minZ + minZ, this.maxX + maxX, this.maxY + maxY, this.maxZ + maxZ);
    }

    public BoundingBox subtract(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new BoundingBox(this.minX - minX, this.minY - minY, this.minZ - minZ, this.maxX - maxX, this.maxY - maxY, this.maxZ - maxZ);
    }

    public boolean intersectsWithBox(Vector3dm vector) {
        return vector.getX() > this.minX && vector.getX() < this.maxX
                && vector.getY() > this.minY && vector.getY() < this.maxY
                && vector.getZ() > this.minZ && vector.getZ() < this.maxZ;
    }

    public Vector3dm getMinimum() {
        return new Vector3dm(minX, minY, minZ);
    }

    public Vector3dm getMaximum() {
        return new Vector3dm(maxX, maxY, maxZ);
    }

    public boolean collides(Vector3dm vector) {
        return (vector.getX() >= this.minX && vector.getX() <= this.maxX) && ((vector.getY() >= this.minY && vector.getY() <= this.maxY) && (vector.getZ() >= this.minZ && vector.getZ() <= this.maxZ));
    }

    public boolean collidesHorizontally(Vector3dm vector) {
        return vector.getX() >= this.minX && vector.getX() <= this.maxX
                && vector.getY() > this.minY && vector.getY() < this.maxY
                && vector.getZ() >= this.minZ && vector.getZ() <= this.maxZ;
    }

    public boolean collidesVertically(Vector3dm vector) {
        return vector.getX() > this.minX && vector.getX() < this.maxX
                && vector.getY() >= this.minY && vector.getY() <= this.maxY
                && vector.getZ() > this.minZ && vector.getZ() < this.maxZ;
    }

    /**
     * if {@code this} and {@code other} overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension. return {@code offsetX} if the bounding boxes do not overlap or if {@code offsetX}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double calculateXOffset(BoundingBox other, double offsetX) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetX > 0.0D && other.maxX <= this.minX) {
                double offset = this.minX - other.maxX;
                if (offset < offsetX) {
                    return offset;
                }
            } else if (offsetX < 0.0D && other.minX >= this.maxX) {
                double offset = this.maxX - other.minX;
                if (offset > offsetX) {
                    return offset;
                }
            }
        }

        return offsetX;
    }

    /**
     * if {@code this} and {@code other} overlap in the X and Z dimensions, calculate the offset between them
     * in the Y dimension. return {@code offsetY} if the bounding boxes do not overlap or if {@code offsetY}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double calculateYOffset(BoundingBox other, double offsetY) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetY > 0.0D && other.maxY <= this.minY) {
                double offset = this.minY - other.maxY;
                if (offset < offsetY) {
                    return offset;
                }
            } else if (offsetY < 0.0D && other.minY >= this.maxY) {
                double offset = this.maxY - other.minY;
                if (offset > offsetY) {
                    return offset;
                }
            }
        }

        return offsetY;
    }

    /**
     * if {@code this} and {@code other} overlap in the Y and X dimensions, calculate the offset between them
     * in the Z dimension. return {@code offsetZ} if the bounding boxes do not overlap or if {@code offsetZ}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double calculateZOffset(BoundingBox other, double offsetZ) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offsetZ > 0.0D && other.maxZ <= this.minZ) {
                double offset = this.minZ - other.maxZ;
                if (offset < offsetZ) {
                    return offset;
                }
            } else if (offsetZ < 0.0D && other.minZ >= this.maxZ) {
                double offset = this.maxZ - other.minZ;
                if (offset > offsetZ) {
                    return offset;
                }
            }
        }

        return offsetZ;
    }

    public BoundingBox addCoord(float x, float y, float z) {
        return new BoundingBox(
                x < 0 ? this.minX + x : this.minX,
                y < 0 ? this.minY + y : this.minY,
                z < 0 ? this.minZ + z : this.minZ,
                x > 0 ? this.maxX + x : this.maxX,
                y > 0 ? this.maxY + y : this.maxY,
                z > 0 ? this.maxZ + z : this.maxZ
        );
    }

    public SimpleCollisionBox toCollisionBox() {
        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public String toString() {
        return "[" + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + "]";
    }
}
