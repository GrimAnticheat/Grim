package ac.grim.grimac.predictionengine.blockeffects.impl;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.blockeffects.BlockCollisions;
import ac.grim.grimac.predictionengine.blockeffects.BlockEffectsResolver;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// 1.21.2-1.21.3
public class BlockEffectsResolverV1_21_2 implements BlockEffectsResolver {

    public static BlockEffectsResolver INSTANCE = new BlockEffectsResolverV1_21_2();

    @Override
    public void applyEffectsFromBlocks(GrimPlayer player, List<GrimPlayer.Movement> movements) {
        LongSet visitedBlocks = player.visitedBlocks;
        SimpleCollisionBox boundingBox = (player.inVehicle()
                ? GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z)
                : player.boundingBox.copy()).expand(-1.0E-5F);

        for (GrimPlayer.Movement movement : movements) {
            Vector3d from = movement.from();
            Vector3d to = movement.to();

            for (Vector3i blockPos : boxTraverseBlocks(from, to, boundingBox)) {
                WrappedBlockState blockState = player.compensatedWorld.getBlock(blockPos);
                StateType blockType = blockState.getType();

                if (blockType.isAir()) {
                    continue;
                }

                if (visitedBlocks.add(GrimMath.asLong(blockPos))) {
                    Collisions.onInsideBlock(player, blockType, blockState, blockPos.x, blockPos.y, blockPos.z, true);
                }
            }
        }

        visitedBlocks.clear();
    }

    private static Iterable<Vector3i> boxTraverseBlocks(Vector3d start, Vector3d end, SimpleCollisionBox boundingBox) {
        Vector3d direction = end.subtract(start);
        Iterable<Vector3i> initialBlocks = SimpleCollisionBox.betweenClosed(boundingBox);
        if (direction.lengthSquared() < GrimMath.square(0.99999F)) {
            return initialBlocks;
        } else {
            Set<Vector3i> traversedBlocks = new ObjectLinkedOpenHashSet<>();
            Vector3d normalizedDirection = direction.normalize().multiply(Collisions.COLLISION_EPSILON);
            Vector3d boxMinPosition = boundingBox.min().toVector3d().add(normalizedDirection);
            Vector3d subtractedMinPosition = boundingBox.min().toVector3d().subtract(direction).subtract(normalizedDirection);
            addCollisionsAlongTravel(traversedBlocks, subtractedMinPosition, boxMinPosition, boundingBox);

            for (Vector3i blockPos : initialBlocks) {
                traversedBlocks.add(blockPos);
            }

            return traversedBlocks;
        }
    }

    public static void addCollisionsAlongTravel(Set<Vector3i> output, Vector3d start, Vector3d end, SimpleCollisionBox boundingBox) {
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
                            output.add(new Vector3i(x, y, z));
                        }
                    }
                }
            }
        }
    }

}
