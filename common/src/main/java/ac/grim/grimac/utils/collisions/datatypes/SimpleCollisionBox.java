package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.predictionengine.blockeffects.BlockCollisions;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.Ray;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import java.util.List;

public class SimpleCollisionBox implements CollisionBox {

    public static final double COLLISION_EPSILON = 1.0E-7;

    public double minX, minY, minZ, maxX, maxY, maxZ;
    private final SimpleCollisionBox[] boxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
    private boolean isFullBlock = false;

    public SimpleCollisionBox() {
        this(0, 0, 0, 0, 0, 0, false);
    }

    /**
     * Creates a box defined by two points in 3d space; used to represent hitboxes and collision boxes.
     * If your min/max values are > 1 you should probably check out {@link HexCollisionBox}
     *
     * @param minX      x position of first corner
     * @param minY      y position of first corner
     * @param minZ      z position of first corner
     * @param maxX      x position of second corner
     * @param maxY      y position of second corner
     * @param maxZ      z position of second corner
     * @param fullBlock - whether on not the box is a perfect 1x1x1 sized block
     */
    public SimpleCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean fullBlock) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        isFullBlock = fullBlock;
    }

    public SimpleCollisionBox(Vector3dm min, Vector3dm max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public SimpleCollisionBox(Vector3i pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    // If you want to set a full block from a point
    public SimpleCollisionBox(double minX, double minY, double minZ) {
        this(minX, minY, minZ, minX + 1, minY + 1, minZ + 1, true);
    }

    /**
     * Creates a box defined by two points in 3d space; used to represent hitboxes and collision boxes.
     * If your min/max values are > 1 you should probably check out {@link HexCollisionBox}
     * Use only if you don't know the fullBlock status, which is rare
     *
     * @param minX x position of first corner
     * @param minY y position of first corner
     * @param minZ z position of first corner
     * @param maxX x position of second corner
     * @param maxY y position of second corner
     * @param maxZ z position of second corner
     */
    public SimpleCollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        if (minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1)
            isFullBlock = true;
    }

    public SimpleCollisionBox(Vector3d min, Vector3d max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public SimpleCollisionBox(Location loc, double width, double height) {
        this(loc.toVector(), width, height);
    }

    public SimpleCollisionBox(Vector3dm vec, double width, double height) {
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

    public Vector3dm[] corners() {
        Vector3dm[] vectors = new Vector3dm[8];
        vectors[0] = new Vector3dm(minX, minY, minZ);
        vectors[1] = new Vector3dm(minX, minY, maxZ);
        vectors[2] = new Vector3dm(maxX, minY, minZ);
        vectors[3] = new Vector3dm(maxX, minY, maxZ);
        vectors[4] = new Vector3dm(minX, maxY, minZ);
        vectors[5] = new Vector3dm(minX, maxY, maxZ);
        vectors[6] = new Vector3dm(maxX, maxY, minZ);
        vectors[7] = new Vector3dm(maxX, maxY, maxZ);
        return vectors;
    }

    public CollisionBox encompass(SimpleCollisionBox other) {
        this.minX = Math.min(this.minX, other.minX);
        this.minY = Math.min(this.minY, other.minY);
        this.minZ = Math.min(this.minZ, other.minZ);
        this.maxX = Math.max(this.maxX, other.maxX);
        this.maxY = Math.max(this.maxY, other.maxY);
        this.maxZ = Math.max(this.maxZ, other.maxZ);
        return this;
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
    public CollisionBox union(SimpleCollisionBox other) {
        return new ComplexCollisionBox(2, this, other);
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

        int size = other.downCast(boxes);

        for (int i = 0; i < size; i++) {
            if (isIntersected(boxes[i])) return true;
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
    public int downCast(SimpleCollisionBox[] list) {
        list[0] = this;
        return 1;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isFullBlock() {
        return isFullBlock;
    }

    @Override
    public boolean isSideFullBlock(BlockFace axis) {
        if (isFullBlock) {
            return true;
        }

        // Get the direction of block we are trying to connect to -> towards the block that is trying to connect
        final BlockFace faceToSourceConnector = axis.getOppositeFace();
        return switch (faceToSourceConnector) {
            case EAST, WEST -> this.minX == 0 && this.maxX == 1;
            case UP, DOWN -> this.minY == 0 && this.maxY == 1;
            case NORTH, SOUTH -> this.minZ == 0 && this.maxZ == 1;
            default -> false;
        };

    }

    public boolean isFullBlockNoCache() {
        return minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1;
    }

    /**
     * if {@code this} and {@code other} overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension. return {@code offsetX} if the bounding boxes do not overlap or if {@code offsetX}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double collideX(SimpleCollisionBox other, double offsetX) {
        if (offsetX != 0 && (other.minY - maxY) < -COLLISION_EPSILON && (other.maxY - minY) > COLLISION_EPSILON &&
                (other.minZ - maxZ) < -COLLISION_EPSILON && (other.maxZ - minZ) > COLLISION_EPSILON) {
            if (offsetX >= 0.0) {
                double max_move = minX - other.maxX; // < 0.0 if no strict collision
                return max_move < -COLLISION_EPSILON ? offsetX : Math.min(max_move, offsetX);
            } else {
                double max_move = maxX - other.minX; // > 0.0 if no strict collision
                return max_move > COLLISION_EPSILON ? offsetX : Math.max(max_move, offsetX);
            }
        }
        return offsetX;
    }

    /**
     * if {@code this} and {@code other} overlap in the X and Z dimensions, calculate the offset between them
     * in the Y dimension. return {@code offsetY} if the bounding boxes do not overlap or if {@code offsetY}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double collideY(SimpleCollisionBox other, double offsetY) {
        if (offsetY != 0 && (other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
                (other.minZ - maxZ) < -COLLISION_EPSILON && (other.maxZ - minZ) > COLLISION_EPSILON) {
            if (offsetY >= 0.0) {
                double max_move = minY - other.maxY; // < 0.0 if no strict collision
                return max_move < -COLLISION_EPSILON ? offsetY : Math.min(max_move, offsetY);
            } else {
                double max_move = maxY - other.minY; // > 0.0 if no strict collision
                return max_move > COLLISION_EPSILON ? offsetY : Math.max(max_move, offsetY);
            }
        }
        return offsetY;
    }

    /**
     * if {@code this} and {@code other} overlap in the Y and X dimensions, calculate the offset between them
     * in the Z dimension. return {@code offsetZ} if the bounding boxes do not overlap or if {@code offsetZ}
     * is closer to {@code 0} then the calculated offset. Otherwise return the calculated offset.
     */
    public double collideZ(SimpleCollisionBox other, double offsetZ) {
        if (offsetZ != 0 && (other.minX - maxX) < -COLLISION_EPSILON && (other.maxX - minX) > COLLISION_EPSILON &&
                (other.minY - maxY) < -COLLISION_EPSILON && (other.maxY - minY) > COLLISION_EPSILON) {
            if (offsetZ >= 0.0) {
                double max_move = minZ - other.maxZ; // < 0.0 if no strict collision
                return max_move < -COLLISION_EPSILON ? offsetZ : Math.min(max_move, offsetZ);
            } else {
                double max_move = maxZ - other.minZ; // > 0.0 if no strict collision
                return max_move > COLLISION_EPSILON ? offsetZ : Math.max(max_move, offsetZ);
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

    public double distanceX(double x) {
        return x >= this.minX && x <= this.maxX ? 0.0 : Math.min(Math.abs(x - this.minX), Math.abs(x - this.maxX));
    }

    public double distanceY(double y) {
        return y >= this.minY && y <= this.maxY ? 0.0 : Math.min(Math.abs(y - this.minY), Math.abs(y - this.maxY));
    }

    public double distanceZ(double z) {
        return z >= this.minZ && z <= this.maxZ ? 0.0 : Math.min(Math.abs(z - this.minZ), Math.abs(z - this.maxZ));
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
     * @return intersection point on the bounding box (only the first is returned) or null if no intersection
     */
    // Copied from hawk lol
    // I would like to point out that this is magic to me and I have not attempted to understand this code
    public Vector3dm intersectsRay(Ray ray, float minDist, float maxDist) {
        Vector3dm invDir = new Vector3dm(1f / ray.getDirection().getX(), 1f / ray.getDirection().getY(), 1f / ray.getDirection().getZ());

        boolean signDirX = invDir.getX() < 0;
        boolean signDirY = invDir.getY() < 0;
        boolean signDirZ = invDir.getZ() < 0;

        double tmin = ((signDirX ? maxX : minX) - ray.getOrigin().getX()) * invDir.getX();
        double tmax = ((signDirX ? minX : maxX) - ray.getOrigin().getX()) * invDir.getX();
        double tymin = ((signDirY ? maxY : minY) - ray.getOrigin().getY()) * invDir.getY();
        double tymax = ((signDirY ? minY : maxY) - ray.getOrigin().getY()) * invDir.getY();

        if (tmin > tymax || tymin > tmax) {
            return null;
        }

        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        double tzmin = ((signDirZ ? maxZ : minZ) - ray.getOrigin().getZ()) * invDir.getZ();
        double tzmax = ((signDirZ ? minZ : maxZ) - ray.getOrigin().getZ()) * invDir.getZ();

        if ((tmin > tzmax) || (tzmin > tmax)) {
            return null;
        }

        if (tzmin > tmin) tmin = tzmin;
        if (tzmax < tmax) tmax = tzmax;

        return tmin < maxDist && tmax > minDist ? ray.getPointAtDistance(tmin) : null;
    }

    public Vector3dm max() {
        return new Vector3dm(maxX, maxY, maxZ);
    }

    public Vector3dm min() {
        return new Vector3dm(minX, minY, minZ);
    }

    public Vector3d getCenter() {
        return new Vector3d(GrimMath.lerp(0.5, this.minX, this.maxX), GrimMath.lerp(0.5, this.minY, this.maxY), GrimMath.lerp(0.5, this.minZ, this.maxZ));
    }

    public DoubleList getYPointPositions() {
        return create(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private DoubleList create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(maxX - minX < COLLISION_EPSILON) && !(maxY - minY < COLLISION_EPSILON) && !(maxZ - minZ < COLLISION_EPSILON)) {
            int xBits = findBits(minX, maxX);
            int yBits = findBits(minY, maxY);
            int zBits = findBits(minZ, maxZ);
            if (xBits < 0 || yBits < 0 || zBits < 0) {
                return DoubleArrayList.wrap(new double[]{minY, maxY});
            } else if (xBits == 0 && yBits == 0 && zBits == 0) {
                return DoubleArrayList.wrap(new double[]{0, 1});
            } else {
                int yFactor = 1 << yBits;

                return new AbstractDoubleList() {
                    @Override
                    public double getDouble(int index) {
                        return (double) index / (double) yFactor;
                    }

                    @Override
                    public int size() {
                        return yFactor + 1;
                    }
                };
            }
        } else {
            return DoubleArrayList.of();
        }
    }

    private int findBits(double min, double max) {
        if (!(min < -COLLISION_EPSILON) && !(max > 1.0000001)) {
            for (int bitShift = 0; bitShift <= 3; bitShift++) {
                int factor = 1 << bitShift;
                double scaledMin = min * (double) factor;
                double scaledMax = max * (double) factor;
                boolean isMinAligned = Math.abs(scaledMin - (double) Math.round(scaledMin)) < COLLISION_EPSILON * (double) factor;
                boolean isMaxAligned = Math.abs(scaledMax - (double) Math.round(scaledMax)) < COLLISION_EPSILON * (double) factor;
                if (isMinAligned && isMaxAligned) {
                    return bitShift;
                }
            }
        }
        return -1;
    }

    public double getXSize() {
        return maxX - minX;
    }

    public double getYSize() {
        return maxY - minY;
    }

    public double getZSize() {
        return maxZ - minZ;
    }

    public SimpleCollisionBox move(Vector3d vector) {
        return this.move(vector.x, vector.y, vector.z);
    }

    public SimpleCollisionBox move(double x, double y, double z) {
        return new SimpleCollisionBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public boolean intersects(SimpleCollisionBox collisionBox) {
        return this.intersects(collisionBox.minX, collisionBox.minY, collisionBox.minZ, collisionBox.maxX, collisionBox.maxY, collisionBox.maxZ);
    }

    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
    }

    public boolean intersects(Vector3d min, Vector3d max) {
        return this.intersects(
                Math.min(min.x, max.x),
                Math.min(min.y, max.y),
                Math.min(min.z, max.z),
                Math.max(min.x, max.x),
                Math.max(min.y, max.y),
                Math.max(min.z, max.z)
        );
    }

    public boolean intersects(Vector3i blockPos) {
        return this.intersects(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
    }

    public static Iterable<Vector3i> betweenClosed(SimpleCollisionBox box) {
        Vector3i startBlockPos = containing(box.minX, box.minY, box.minZ);
        Vector3i endBlockPos = containing(box.maxX, box.maxY, box.maxZ);
        return betweenClosed(startBlockPos, endBlockPos);
    }

    public static Vector3i containing(double x, double y, double z) {
        return new Vector3i(GrimMath.floor(x), GrimMath.floor(y), GrimMath.floor(z));
    }

    public static Iterable<Vector3i> betweenClosed(Vector3i firstPos, Vector3i secondPos) {
        return betweenClosed(
                Math.min(firstPos.getX(), secondPos.getX()),
                Math.min(firstPos.getY(), secondPos.getY()),
                Math.min(firstPos.getZ(), secondPos.getZ()),
                Math.max(firstPos.getX(), secondPos.getX()),
                Math.max(firstPos.getY(), secondPos.getY()),
                Math.max(firstPos.getZ(), secondPos.getZ())
        );
    }

    public static Iterable<Vector3i> betweenClosed(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd) {
        int xRange = xEnd - xStart + 1;
        int yRange = yEnd - yStart + 1;
        int zRange = zEnd - zStart + 1;
        int totalVectors = xRange * yRange * zRange;
        return () -> new AbstractIterator<>() {
            private int index;

            @Override
            protected Vector3i computeNext() {
                if (this.index == totalVectors) {
                    return this.endOfData();
                } else {
                    int xOffset = this.index % xRange;
                    int yOffset = this.index / xRange;
                    int yOffsetMod = yOffset % yRange;
                    int zOffset = yOffset / yRange;
                    this.index++;
                    return new Vector3i(xStart + xOffset, yStart + yOffsetMod, zStart + zOffset);
                }
            }
        };
    }

    public static Iterable<Vector3i> betweenCornersInDirection(SimpleCollisionBox boundingBox, Vector3d directionVector) {
        Vector3d min = boundingBox.min().toVector3d();
        int minX = GrimMath.floor(min.x);
        int minY = GrimMath.floor(min.y);
        int minZ = GrimMath.floor(min.z);
        Vector3d max = boundingBox.max().toVector3d();
        int maxX = GrimMath.floor(max.x);
        int maxY = GrimMath.floor(max.y);
        int maxZ = GrimMath.floor(max.z);
        return betweenCornersInDirection(minX, minY, minZ, maxX, maxY, maxZ, directionVector);
    }

    public static Iterable<Vector3i> betweenCornersInDirection(Vector3i min, Vector3i max, Vector3d directionVector) {
        return betweenCornersInDirection(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), directionVector);
    }

    public static Iterable<Vector3i> betweenCornersInDirection(int x1, int y1, int z1, int x2, int y2, int z2, Vector3d directionVector) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        int sizeX = maxX - minX;
        int sizeY = maxY - minY;
        int sizeZ = maxZ - minZ;

        int startX = directionVector.x >= 0.0 ? minX : maxX;
        int startY = directionVector.y >= 0.0 ? minY : maxY;
        int startZ = directionVector.z >= 0.0 ? minZ : maxZ;

        List<Collisions.Axis> axisOrder = BlockCollisions.axisStepOrder(directionVector);
        Collisions.Axis primaryAxis = axisOrder.get(0);
        Collisions.Axis secondaryAxis = axisOrder.get(1);
        Collisions.Axis tertiaryAxis = axisOrder.get(2);

        Direction primaryDirection = primaryAxis.get(directionVector) >= 0.0 ? primaryAxis.getPositive() : primaryAxis.getNegative();
        Direction secondaryDirection = secondaryAxis.get(directionVector) >= 0.0 ? secondaryAxis.getPositive() : secondaryAxis.getNegative();
        Direction tertiaryDirection = tertiaryAxis.get(directionVector) >= 0.0 ? tertiaryAxis.getPositive() : tertiaryAxis.getNegative();

        int primaryCount = primaryAxis.choose(sizeX, sizeY, sizeZ);
        int secondaryCount = secondaryAxis.choose(sizeX, sizeY, sizeZ);
        int tertiaryCount = tertiaryAxis.choose(sizeX, sizeY, sizeZ);

        return () -> new AbstractIterator<>() {
            private int firstIndex;
            private int secondIndex;
            private int thirdIndex;
            private boolean end;
            private final int firstDirX = primaryDirection.getVector().getX();
            private final int firstDirY = primaryDirection.getVector().getY();
            private final int firstDirZ = primaryDirection.getVector().getZ();
            private final int secondDirX = secondaryDirection.getVector().getX();
            private final int secondDirY = secondaryDirection.getVector().getY();
            private final int secondDirZ = secondaryDirection.getVector().getZ();
            private final int thirdDirX = tertiaryDirection.getVector().getX();
            private final int thirdDirY = tertiaryDirection.getVector().getY();
            private final int thirdDirZ = tertiaryDirection.getVector().getZ();

            protected Vector3i computeNext() {
                if (this.end) {
                    return this.endOfData();
                } else {
                    Vector3i cursor = new Vector3i(
                            startX + this.firstDirX * this.firstIndex + this.secondDirX * this.secondIndex + this.thirdDirX * this.thirdIndex,
                            startY + this.firstDirY * this.firstIndex + this.secondDirY * this.secondIndex + this.thirdDirY * this.thirdIndex,
                            startZ + this.firstDirZ * this.firstIndex + this.secondDirZ * this.secondIndex + this.thirdDirZ * this.thirdIndex
                    );

                    if (this.thirdIndex < tertiaryCount) {
                        this.thirdIndex++;
                    } else if (this.secondIndex < secondaryCount) {
                        this.secondIndex++;
                        this.thirdIndex = 0;
                    } else if (this.firstIndex < primaryCount) {
                        this.firstIndex++;
                        this.thirdIndex = 0;
                        this.secondIndex = 0;
                    } else {
                        this.end = true;
                    }

                    return cursor;
                }
            }
        };
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
