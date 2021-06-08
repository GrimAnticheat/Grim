package ac.grim.grimac.utils.collisions.datatypes;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;

public class SimpleCollisionBox implements CollisionBox {
    public static final double COLLISION_EPSILON = 1.0E-7;
    public double minX, minY, minZ, maxX, maxY, maxZ;
    boolean isFullBlock = false;

    public SimpleCollisionBox() {
        this(0, 0, 0, 0, 0, 0);
    }

    public SimpleCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1) isFullBlock = true;

        if (minX < maxX) {
            this.minX = minX;
            this.maxX = maxX;
        } else {
            this.minX = maxX;
            this.maxX = minX;
        }
        if (minY < maxY) {
            this.minY = minY;
            this.maxY = maxY;
        } else {
            this.minY = maxY;
            this.maxY = minY;
        }
        if (minZ < maxZ) {
            this.minZ = minZ;
            this.maxZ = maxZ;
        } else {
            this.minZ = maxZ;
            this.maxZ = minZ;
        }
    }

    public SimpleCollisionBox(Vector min, Vector max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public SimpleCollisionBox(Location loc, double width, double height) {
        this(loc.toVector(), width, height);
    }

    public SimpleCollisionBox(Vector vec, double width, double height) {
        this(vec.getX(), vec.getY(), vec.getZ(), vec.getX(), vec.getY(), vec.getZ());

        expand(width / 2, 0, width / 2);
        maxY += height;
    }

    public SimpleCollisionBox(BoundingBox box) {
        this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public void sort() {
        double temp = 0;
        if (minX >= maxX) {
            temp = minX;
            this.minX = maxX;
            this.maxX = temp;
        }
        if (minY >= maxY) {
            temp = minY;
            this.minY = maxY;
            this.maxY = temp;
        }
        if (minZ >= maxZ) {
            temp = minZ;
            this.minZ = maxZ;
            this.maxZ = temp;
        }
    }

    public SimpleCollisionBox copy() {
        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public SimpleCollisionBox offset(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        list.add(this);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isFullBlock() {
        return isFullBlock;
    }

    public SimpleCollisionBox expandMin(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        return this;
    }

    public SimpleCollisionBox expandMax(double x, double y, double z) {
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public SimpleCollisionBox expand(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public SimpleCollisionBox expand(double value) {
        this.minX -= value;
        this.minY -= value;
        this.minZ -= value;
        this.maxX += value;
        this.maxY += value;
        this.maxZ += value;
        return this;
    }

    public Vector[] corners() {
        sort();
        Vector[] vectors = new Vector[8];
        vectors[0] = new Vector(minX, minY, minZ);
        vectors[1] = new Vector(minX, minY, maxZ);
        vectors[2] = new Vector(maxX, minY, minZ);
        vectors[3] = new Vector(maxX, minY, maxZ);
        vectors[4] = new Vector(minX, maxY, minZ);
        vectors[5] = new Vector(minX, maxY, maxZ);
        vectors[6] = new Vector(maxX, maxY, minZ);
        vectors[7] = new Vector(maxX, maxY, maxZ);
        return vectors;
    }

    public Vector min() {
        return new Vector(minX, minY, minZ);
    }

    public Vector max() {
        return new Vector(maxX, maxY, maxZ);
    }

    public SimpleCollisionBox expandToCoordinate(double x, double y, double z) {
        if (x < 0.0D) {
            minX += x;
        } else {
            maxX += x;
        }

        if (y < 0.0D) {
            minY += y;
        } else {
            maxY += y;
        }

        if (z < 0.0D) {
            minZ += z;
        } else {
            maxZ += z;
        }

        return this;
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        if (other instanceof SimpleCollisionBox) {
            SimpleCollisionBox box = ((SimpleCollisionBox) other);
            box.sort();
            sort();
            return box.maxX >= this.minX && box.minX <= this.maxX
                    && box.maxY >= this.minY && box.minY <= this.maxY
                    && box.maxZ >= this.minZ && box.minZ <= this.maxZ;
        } else {
            return other.isCollided(this);
        }
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        if (other instanceof SimpleCollisionBox) {
            SimpleCollisionBox box = (SimpleCollisionBox) other;
            box.sort();
            sort();
            return box.maxX > this.minX && box.minX < this.maxX
                    && box.maxY > this.minY && box.minY < this.maxY
                    && box.maxZ > this.minZ && box.minZ < this.maxZ;
        } else {
            return other.isIntersected(this);
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double collideX(SimpleCollisionBox other, double offsetX) {
        if (offsetX == 0.0) {
            return 0.0;
        }

        if ((other.minY - maxY) < -COLLISION_EPSILON && (other.maxY - minY) > COLLISION_EPSILON &&
                (other.minZ - maxZ) < -COLLISION_EPSILON && (other.maxZ - minZ) > COLLISION_EPSILON) {

            if (offsetX >= 0.0) {
                double max_move = minX - other.maxX; // < 0.0 if no strict collision
                if (max_move < -COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.min(max_move, offsetX);
            } else {
                double max_move = maxX - other.minX; // > 0.0 if no strict collision
                if (max_move > COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.max(max_move, offsetX);
            }
        }
        return offsetX;
    }

    /**
     * if instance and the argument bounding boxes overlap in the X and Z dimensions, calculate the offset between them
     * in the Y dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double collideY(SimpleCollisionBox other, double offsetY) {
        if (offsetY == 0.0) {
            return 0.0;
        }

        if ((other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
                (other.minZ - maxZ) < -COLLISION_EPSILON && (other.maxZ - minZ) > COLLISION_EPSILON) {
            if (offsetY >= 0.0) {
                double max_move = minY - other.maxY; // < 0.0 if no strict collision
                if (max_move < -COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.min(max_move, offsetY);
            } else {
                double max_move = maxY - other.minY; // > 0.0 if no strict collision
                if (max_move > COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.max(max_move, offsetY);
            }
        }
        return offsetY;
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and X dimensions, calculate the offset between them
     * in the Z dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double collideZ(SimpleCollisionBox other, double offsetZ) {
        if (offsetZ == 0.0) {
            return 0.0;
        }

        if ((other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
                (other.minY - maxY) < -COLLISION_EPSILON && (other.maxY - minY) > COLLISION_EPSILON) {
            if (offsetZ >= 0.0) {
                double max_move = minZ - other.maxZ; // < 0.0 if no strict collision
                if (max_move < -COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.min(max_move, offsetZ);
            } else {
                double max_move = maxZ - other.minZ; // > 0.0 if no strict collision
                if (max_move > COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.max(max_move, offsetZ);
            }
        }
        return offsetZ;
    }

    public double distance(SimpleCollisionBox box) {
        double xwidth = (maxX - minX) / 2, zwidth = (maxZ - minZ) / 2;
        double bxwidth = (box.maxX - box.minX) / 2, bzwidth = (box.maxZ - box.minZ) / 2;
        double hxz = Math.hypot(minX - box.minX, minZ - box.minZ);

        return hxz - (xwidth + zwidth + bxwidth + bzwidth) / 4;
    }

    @Override
    public String toString() {
        return "SimpleCollisionBox{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", maxZ=" + maxZ +
                ", isFullBlock=" + isFullBlock +
                '}';
    }
}