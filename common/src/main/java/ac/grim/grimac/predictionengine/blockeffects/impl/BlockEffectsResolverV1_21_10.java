package ac.grim.grimac.predictionengine.blockeffects.impl;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.blockeffects.BlockCollisions;
import ac.grim.grimac.predictionengine.blockeffects.BlockEffectsResolver;
import ac.grim.grimac.predictionengine.blockeffects.BlockStepVisitor;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

// 1.21.10 ONLY - 1.21.9 is impossible to implement due to Mojang awesomeness
public class BlockEffectsResolverV1_21_10 implements BlockEffectsResolver {

    public static final BlockEffectsResolver INSTANCE = new BlockEffectsResolverV1_21_10();

    @Override
    public void applyEffectsFromBlocks(GrimPlayer player, List<GrimPlayer.Movement> movements) {
        LongSet visitedBlocks = player.visitedBlocks;

        for (GrimPlayer.Movement movement : movements) {
            Vector3d from = movement.from();
            Vector3d to = movement.to().subtract(movement.from());

            int iterationCount = 16;
            if (movement.axisIndependant() && to.lengthSquared() > 0.0) {
                for (Collisions.Axis axis : BlockCollisions.axisStepOrder(movement.axisDependentOriginalMovement())) {
                    double value = axis.get(to);
                    if (value != 0.0) {
                        Vector3d vector = BlockCollisions.relative(from, axis.getPositive(), value);
                        iterationCount -= checkInsideBlocks(player, from, vector, visitedBlocks, iterationCount);
                        from = vector;
                    }
                }
            } else {
                iterationCount -= checkInsideBlocks(player, movement.from(), movement.to(), visitedBlocks, 16);
            }

            if (iterationCount <= 0) {
                checkInsideBlocks(player, movement.to(), movement.to(), visitedBlocks, 1);
            }
        }

        visitedBlocks.clear();
    }

    public static int checkInsideBlocks(GrimPlayer player, Vector3d from, Vector3d to, LongSet visitedBlocks, int count) {
        SimpleCollisionBox boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, to.x, to.y, to.z).expand(-1.0E-5F);
        boolean isFarEnough = from.distanceSquared(to) > GrimMath.square(0.9999900000002526);

        AtomicInteger blockCount = new AtomicInteger();
        forEachBlockIntersectedBetween(from, to, boundingBox, (blockPos, localCount) -> {
            if (localCount >= count) {
                return false;
            }

            blockCount.set(localCount);

            WrappedBlockState blockState = player.compensatedWorld.getBlock(blockPos);
            StateType blockType = blockState.getType();

            if (blockType.isAir()) {
                return true;
            }

            if (visitedBlocks.add(GrimMath.asLong(blockPos))) {
                boolean shouldApply = isFarEnough || boundingBox.intersects(blockPos);
                Collisions.onInsideBlock(player, blockType, blockState, blockPos.x, blockPos.y, blockPos.z, shouldApply);
            }

            return true;
        });

        return blockCount.get() + 1;
    }

    public static boolean forEachBlockIntersectedBetween(Vector3d start, Vector3d end, SimpleCollisionBox boundingBox, BlockStepVisitor visitor) {
        Vector3d direction = end.subtract(start);
        if (direction.lengthSquared() < (double) GrimMath.square(1.0E-5F)) {
            for (Vector3i blockPos : SimpleCollisionBox.betweenClosed(boundingBox)) {
                if (!visitor.visit(blockPos, 0)) {
                    return false;
                }
            }

            return true;
        } else {
            LongSet alreadyVisited = new LongOpenHashSet();

            for (Vector3i blockPos : SimpleCollisionBox.betweenCornersInDirection(boundingBox.move(direction.multiply(-1.0)), direction)) {
                if (!visitor.visit(blockPos, 0)) {
                    return false;
                }

                alreadyVisited.add(GrimMath.asLong(blockPos));
            }

            int iterationCount = addCollisionsAlongTravel(alreadyVisited, direction, boundingBox, visitor);
            if (iterationCount < 0) {
                return false;
            } else {
                for (Vector3i blockPos : SimpleCollisionBox.betweenCornersInDirection(boundingBox, direction)) {
                    if (alreadyVisited.add(GrimMath.asLong(blockPos)) && !visitor.visit(blockPos, iterationCount + 1)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static int addCollisionsAlongTravel(LongSet alreadyVisited, Vector3d direction, SimpleCollisionBox boundingBox, BlockStepVisitor visitor) {
        double sizeX = boundingBox.getXSize();
        double sizeY = boundingBox.getYSize();
        double sizeZ = boundingBox.getZSize();

        Vector3i furthestCorner = BlockCollisions.getFurthestCorner(direction);

        Vector3d center = boundingBox.getCenter();
        Vector3d end = new Vector3d(
                center.x + sizeX * 0.5 * furthestCorner.getX(),
                center.y + sizeY * 0.5 * furthestCorner.getY(),
                center.z + sizeZ * 0.5 * furthestCorner.getZ()
        );
        Vector3d start = end.subtract(direction);

        int currentX = GrimMath.floor(start.x);
        int currentY = GrimMath.floor(start.y);
        int currentZ = GrimMath.floor(start.z);
        int stepX = GrimMath.sign(direction.x);
        int stepY = GrimMath.sign(direction.y);
        int stepZ = GrimMath.sign(direction.z);
        double tMaxX = stepX == 0 ? Double.MAX_VALUE : stepX / direction.x;
        double tMaxY = stepY == 0 ? Double.MAX_VALUE : stepY / direction.y;
        double tMaxZ = stepZ == 0 ? Double.MAX_VALUE : stepZ / direction.z;
        double tDeltaX = tMaxX * (stepX > 0 ? 1.0 - GrimMath.frac(start.x) : GrimMath.frac(start.x));
        double tDeltaY = tMaxY * (stepY > 0 ? 1.0 - GrimMath.frac(start.y) : GrimMath.frac(start.y));
        double tDeltaZ = tMaxZ * (stepZ > 0 ? 1.0 - GrimMath.frac(start.z) : GrimMath.frac(start.z));
        int iterationCount = 0;

        while (tDeltaX <= 1.0 || tDeltaY <= 1.0 || tDeltaZ <= 1.0) {
            if (tDeltaX < tDeltaY) {
                if (tDeltaX < tDeltaZ) {
                    currentX += stepX;
                    tDeltaX += tMaxX;
                } else {
                    currentZ += stepZ;
                    tDeltaZ += tMaxZ;
                }
            } else if (tDeltaY < tDeltaZ) {
                currentY += stepY;
                tDeltaY += tMaxY;
            } else {
                currentZ += stepZ;
                tDeltaZ += tMaxZ;
            }

            Optional<Vector3d> collisionPoint = BlockCollisions.clip(currentX, currentY, currentZ, currentX + 1, currentY + 1, currentZ + 1, start, end);
            if (!collisionPoint.isEmpty()) {
                iterationCount++;
                Vector3d collisionVec = collisionPoint.get();
                double clampedX = GrimMath.clamp(collisionVec.x, currentX + 1.0E-5F, currentX + 1.0 - 1.0E-5F);
                double clampedY = GrimMath.clamp(collisionVec.y, currentY + 1.0E-5F, currentY + 1.0 - 1.0E-5F);
                double clampedZ = GrimMath.clamp(collisionVec.z, currentZ + 1.0E-5F, currentZ + 1.0 - 1.0E-5F);
                int endX = GrimMath.floor(clampedX - sizeX * furthestCorner.getX());
                int endY = GrimMath.floor(clampedY - sizeY * furthestCorner.getY());
                int endZ = GrimMath.floor(clampedZ - sizeZ * furthestCorner.getZ());
                int copyIterationCount = iterationCount;

                for (Vector3i blockPos : SimpleCollisionBox.betweenCornersInDirection(currentX, currentY, currentZ, endX, endY, endZ, direction)) {
                    if (alreadyVisited.add(GrimMath.asLong(blockPos)) && !visitor.visit(blockPos, copyIterationCount)) {
                        return -1;
                    }
                }
            }
        }

        return iterationCount;
    }

}
