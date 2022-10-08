package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.utils.nmsutil.Ray;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SimpleCollisionBox implements CollisionBox {
    public static final double COLLISION_EPSILON = 1.0E-7;
    public double minX, minY, minZ, maxX, maxY, maxZ;
    boolean isFullBlock = false;

    public SimpleCollisionBox() {
        this(0, 0, 0, 0, 0, 0, false);
    }

    public SimpleCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean fullBlock) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        isFullBlock = fullBlock;
    }

    public SimpleCollisionBox(Vector min, Vector max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public SimpleCollisionBox(Vector3i pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    // If you want to set a full block from a point
    public SimpleCollisionBox(double minX, double minY, double minZ) {
        this(minX, minY, minZ, minX + 1, minY + 1, minZ + 1, true);
    }

    // Use only if you don't know the fullBlock status, which is rare
    public SimpleCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        if (minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1) isFullBlock = true;
    }

    public SimpleCollisionBox(Vector3d min, Vector3d max) {
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

    public SimpleCollisionBox expand(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return sort();
    }

    public SimpleCollisionBox sort() {
        double minX = Math.min(this.minX, this.maxX);
        double minY = Math.min(this.minY, this.maxY);
        double minZ = Math.min(this.minZ, this.maxZ);
        double maxX = Math.max(this.minX, this.maxX);
        double maxY = Math.max(this.minY, this.maxY);
        double maxZ = Math.max(this.minZ, this.maxZ);

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        return this;
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

    public SimpleCollisionBox expandToAbsoluteCoordinates(double x, double y, double z) {
        return expandToCoordinate(x - ((minX + maxX) / 2), y - ((minY + maxY) / 2), z - ((minZ + maxZ) / 2));
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

    public SimpleCollisionBox combineToMinimum(double x, double y, double z) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);

        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);

        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);

        return this;
    }

    @Override
    public boolean isCollided(SimpleCollisionBox other) {
        return other.maxX >= this.minX && other.minX <= this.maxX
                && other.maxY >= this.minY && other.minY <= this.maxY
                && other.maxZ >= this.minZ && other.minZ <= this.maxZ;
    }

    @Override
    public boolean isIntersected(SimpleCollisionBox other) {
        return other.maxX - SimpleCollisionBox.COLLISION_EPSILON > this.minX && other.minX + SimpleCollisionBox.COLLISION_EPSILON < this.maxX
                && other.maxY - SimpleCollisionBox.COLLISION_EPSILON > this.minY && other.minY + SimpleCollisionBox.COLLISION_EPSILON < this.maxY
                && other.maxZ - SimpleCollisionBox.COLLISION_EPSILON > this.minZ && other.minZ + SimpleCollisionBox.COLLISION_EPSILON < this.maxZ;
    }

    public boolean isIntersected(CollisionBox other) {
        // Optimization - don't allocate a list if this is just a SimpleCollisionBox
        if (other instanceof SimpleCollisionBox) {
            return isIntersected((SimpleCollisionBox) other);
        }

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        other.downCast(boxes);

        for (SimpleCollisionBox box : boxes) {
            if (isIntersected(box)) return true;
        }

        return false;
    }

    public boolean collidesVertically(SimpleCollisionBox other) {
        return other.maxX > this.minX && other.minX < this.maxX
                && other.maxY >= this.minY && other.minY <= this.maxY
                && other.maxZ > this.minZ && other.minZ < this.maxZ;
    }

    public SimpleCollisionBox copy() {
        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ, isFullBlock);
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

    public boolean isFullBlockNoCache() {
        return minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1;
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double collideX(SimpleCollisionBox other, double offsetX) {
        if (offsetX != 0 && (other.minY - maxY) < -COLLISION_EPSILON && (other.maxY - minY) > COLLISION_EPSILON &&
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
        if (offsetY != 0 && (other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
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
        if (offsetZ != 0 && (other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
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

    /**
     * Calculates intersection with the given ray between a certain distance
     * interval.
     * <p>
     * Ray-box intersection is using IEEE numerical properties to ensure the
     * test is both robust and efficient, as described in:
     * <p>
     * Amy Williams, Steve Barrus, R. Keith Morley, and Peter Shirley: "An
     * Efficient and Robust Ray-Box Intersection Algorithm" Journal of graphics
     * tools, 10(1):49-54, 2005
     *
     * @param ray     incident ray
     * @param minDist minimum distance
     * @param maxDist maximum distance
     * @return intersection point on the bounding box (only the first is
     * returned) or null if no intersection
     */
    // Copied from hawk lol
    // I would like to point out that this is magic to me and I have not attempted to understand this code
    public Vector intersectsRay(Ray ray, float minDist, float maxDist) {
        Vector invDir = new Vector(1f / ray.calculateDirection().getX(), 1f / ray.calculateDirection().getY(), 1f / ray.calculateDirection().getZ());

        boolean signDirX = invDir.getX() < 0;
        boolean signDirY = invDir.getY() < 0;
        boolean signDirZ = invDir.getZ() < 0;

        Vector bbox = signDirX ? max() : min();
        double tmin = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirX ? min() : max();
        double tmax = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirY ? max() : min();
        double tymin = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();
        bbox = signDirY ? min() : max();
        double tymax = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();

        if ((tmin > tymax) || (tymin > tmax)) {
            return null;
        }
        if (tymin > tmin) {
            tmin = tymin;
        }
        if (tymax < tmax) {
            tmax = tymax;
        }

        bbox = signDirZ ? max() : min();
        double tzmin = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();
        bbox = signDirZ ? min() : max();
        double tzmax = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();

        if ((tmin > tzmax) || (tzmin > tmax)) {
            return null;
        }
        if (tzmin > tmin) {
            tmin = tzmin;
        }
        if (tzmax < tmax) {
            tmax = tzmax;
        }
        if ((tmin < maxDist) && (tmax > minDist)) {
            return ray.getPointAtDistance(tmin);
        }
        return null;
    }

    public Vector max() {
        return new Vector(maxX, maxY, maxZ);
    }

    public Vector min() {
        return new Vector(minX, minY, minZ);
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