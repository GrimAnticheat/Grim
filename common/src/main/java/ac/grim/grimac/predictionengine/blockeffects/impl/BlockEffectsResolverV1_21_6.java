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

// 1.21.6-1.21.8
public class BlockEffectsResolverV1_21_6 implements BlockEffectsResolver {

    public static final BlockEffectsResolver INSTANCE = new BlockEffectsResolverV1_21_6();

    @Override
    public void applyEffectsFromBlocks(GrimPlayer player, List<GrimPlayer.Movement> movements) {
        LongSet visitedBlocks = player.visitedBlocks;

        for (GrimPlayer.Movement movement : movements) {
            Vector3d from = movement.from();
            Vector3d to = movement.to().subtract(movement.from());
            if (movement.axisIndependant() && to.lengthSquared() > 0.0) {
                for (Collisions.Axis axis : BlockCollisions.axisStepOrder(to)) {
                    double value = axis.get(to);
                    if (value != 0.0) {
                        Vector3d vector = BlockCollisions.relative(from, axis.getPositive(), value);
                        checkInsideBlocks(player, from, vector, visitedBlocks);
                        from = vector;
                    }
                }
            } else {
                checkInsideBlocks(player, movement.from(), movement.to(), visitedBlocks);
            }
        }

        visitedBlocks.clear();
    }

    private static void checkInsideBlocks(GrimPlayer player, Vector3d from, Vector3d to, LongSet visitedBlocks) {
        SimpleCollisionBox boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, to.x, to.y, to.z).expand(-1.0E-5F);
        forEachBlockIntersectedBetween(from, to, boundingBox, (blockPos, i) -> {
            WrappedBlockState blockState = player.compensatedWorld.getBlock(blockPos);
            StateType blockType = blockState.getType();

            if (blockType.isAir()) {
                return true;
            }

            if (visitedBlocks.add(GrimMath.asLong(blockPos))) {
                Collisions.onInsideBlock(player, blockType, blockState, blockPos.x, blockPos.y, blockPos.z, true);
            }

            return true;
        });
    }

    private static boolean forEachBlockIntersectedBetween(Vector3d start, Vector3d end, SimpleCollisionBox boundingBox, BlockStepVisitor blockStepVisitor) {
        Vector3d direction = end.subtract(start);
        if (direction.lengthSquared() < GrimMath.square(0.99999F)) {
            for (Vector3i blockPos : SimpleCollisionBox.betweenClosed(boundingBox)) {
                if (!blockStepVisitor.visit(blockPos, 0)) {
                    return false;
                }
            }

            return true;
        } else {
            LongSet alreadyVisited = new LongOpenHashSet();
            Vector3d boxMinPosition = boundingBox.min().toVector3d();
            Vector3d subtractedMinPosition = boxMinPosition.subtract(direction);

            int iterationCount = addCollisionsAlongTravel(alreadyVisited, subtractedMinPosition, boxMinPosition, boundingBox, blockStepVisitor);
            if (iterationCount < 0) {
                return false;
            } else {
                for (Vector3i blockPos : SimpleCollisionBox.betweenClosed(boundingBox)) {
                    if (!alreadyVisited.contains(GrimMath.asLong(blockPos)) && !blockStepVisitor.visit(blockPos, iterationCount + 1)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private static int addCollisionsAlongTravel(LongSet alreadyVisited, Vector3d start, Vector3d end, SimpleCollisionBox boundingBox, BlockStepVisitor blockStepVisitor) {
        Vector3d direction = end.subtract(start);
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

            if (iterationCount++ > 16) {
                break;
            }

            Optional<Vector3d> collisionPoint = BlockCollisions.clip(currentX, currentY, currentZ, currentX + 1, currentY + 1, currentZ + 1, start, end);
            if (!collisionPoint.isEmpty()) {
                Vector3d collisionVec = collisionPoint.get();
                double clampedX = GrimMath.clamp(collisionVec.x, currentX + 1.0E-5F, currentX + 1.0 - 1.0E-5F);
                double clampedY = GrimMath.clamp(collisionVec.y, currentY + 1.0E-5F, currentY + 1.0 - 1.0E-5F);
                double clampedZ = GrimMath.clamp(collisionVec.z, currentZ + 1.0E-5F, currentZ + 1.0 - 1.0E-5F);
                int endX = GrimMath.floor(clampedX + boundingBox.getXSize());
                int endY = GrimMath.floor(clampedY + boundingBox.getYSize());
                int endZ = GrimMath.floor(clampedZ + boundingBox.getZSize());

                for (int x = currentX; x <= endX; x++) {
                    for (int y = currentY; y <= endY; y++) {
                        for (int z = currentZ; z <= endZ; z++) {
                            if (alreadyVisited.add(GrimMath.asLong(x, y, z)) && !blockStepVisitor.visit(new Vector3i(x, y, z), iterationCount)) {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        return iterationCount;
    }

}
