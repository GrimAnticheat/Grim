package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public class BlockRayTrace {
    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    public static Pair<int[], BlockFace> traverseBlocksLOSP(GrimPlayer player, double[] start, double[] end, BiFunction<WrappedBlockState, int[], Pair<int[], BlockFace>> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end[0], start[0]);
        double endY = GrimMath.lerp(-1.0E-7D, end[1], start[1]);
        double endZ = GrimMath.lerp(-1.0E-7D, end[2], start[2]);
        double startX = GrimMath.lerp(-1.0E-7D, start[0], end[0]);
        double startY = GrimMath.lerp(-1.0E-7D, start[1], end[1]);
        double startZ = GrimMath.lerp(-1.0E-7D, start[2], end[2]);

        int[] floorStart = new int[]{GrimMath.floor(startX), GrimMath.floor(startY), GrimMath.floor(startZ)};

        if (start[0] == end[0] && start[1] == end[1] && start[2] == end[2]) return null;

        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStart[0], floorStart[1], floorStart[2]);
        Pair<int[], BlockFace> apply = predicate.apply(state, floorStart);

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        double xSign = Math.signum(xDiff);
        double ySign = Math.signum(yDiff);
        double zSign = Math.signum(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double tMaxX = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double tMaxY = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double tMaxZ = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // tMax represents the maximum distance along each axis before crossing a block boundary
        // The loop continues as long as the ray hasn't reached its end point along at least one axis.
        // In each iteration, it moves to the next block boundary along the axis with the smallest tMax value,
        // updates the corresponding coordinate, and checks for a hit in the new block, Google "3D DDA" for more info
        while (tMaxX <= 1.0D || tMaxY <= 1.0D || tMaxZ <= 1.0D) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    floorStart[0] += xSign;
                    tMaxX += posXInverse;
                } else {
                    floorStart[2] += zSign;
                    tMaxZ += posZInverse;
                }
            } else if (tMaxY < tMaxZ) {
                floorStart[1] += ySign;
                tMaxY += posYInverse;
            } else {
                floorStart[2] += zSign;
                tMaxZ += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStart[0], floorStart[1], floorStart[2]);
            apply = predicate.apply(state, floorStart);

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    public static HitData traverseBlocks(GrimPlayer player, double[] start, double[] end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end[0], start[0]);
        double endY = GrimMath.lerp(-1.0E-7D, end[1], start[1]);
        double endZ = GrimMath.lerp(-1.0E-7D, end[2], start[2]);
        double startX = GrimMath.lerp(-1.0E-7D, start[0], end[0]);
        double startY = GrimMath.lerp(-1.0E-7D, start[1], end[1]);
        double startZ = GrimMath.lerp(-1.0E-7D, start[2], end[2]);
        int floorStartX = GrimMath.floor(startX);
        int floorStartY = GrimMath.floor(startY);
        int floorStartZ = GrimMath.floor(startZ);

        if (start[0] == end[0] && start[1] == end[1] && start[2] == end[2]) return null;

        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
        HitData apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        double xSign = Math.signum(xDiff);
        double ySign = Math.signum(yDiff);
        double zSign = Math.signum(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double tMaxX = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double tMaxY = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double tMaxZ = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // tMax represents the maximum distance along each axis before crossing a block boundary
        // The loop continues as long as the ray hasn't reached its end point along at least one axis.
        // In each iteration, it moves to the next block boundary along the axis with the smallest tMax value,
        // updates the corresponding coordinate, and checks for a hit in the new block, Google "3D DDA" for more info
        while (tMaxX <= 1.0D || tMaxY <= 1.0D || tMaxZ <= 1.0D) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    floorStartX += xSign;
                    tMaxX += posXInverse;
                } else {
                    floorStartZ += zSign;
                    tMaxZ += posZInverse;
                }
            } else if (tMaxY < tMaxZ) {
                floorStartY += ySign;
                tMaxY += posYInverse;
            } else {
                floorStartZ += zSign;
                tMaxZ += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
            apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    public static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        return traverseBlocks(player, new double[]{start.x, start.y, start.z}, new double[]{end.x, end.y, end.z}, predicate);
    }

    public static Pair<int[], BlockFace> getNearestReachHitResult(GrimPlayer player, double[] eyePos, double[] lookVec, double currentDistance, double maxDistance, int[] targetBlockVec, BlockFace expectedBlockFace) {
        double[] endPos = new double[]{
                eyePos[0] + lookVec[0] * maxDistance,
                eyePos[1] + lookVec[1] * maxDistance,
                eyePos[2] + lookVec[2] * maxDistance
        };

        double[] currentEnd = new double[]{
                eyePos[0] + lookVec[0] * currentDistance,
                eyePos[1] + lookVec[1] * currentDistance,
                eyePos[2] + lookVec[2] * currentDistance
        };

        return traverseBlocksLOSP(player, eyePos, endPos, (block, vector3i) -> {
            ClientVersion clientVersion = player.getClientVersion();
            CollisionBox data = HitboxData.getBlockHitbox(player, null, clientVersion, block, vector3i[0], vector3i[1], vector3i[2]);
            if (data == NoCollisionBox.INSTANCE) return null;
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            double[] bestHitLoc = null;
            BlockFace bestFace = null;

            // BEWARE OF https://bugs.mojang.com/browse/MC-85109 FOR 1.8 PLAYERS
            // 1.8 Brewing Stand hitbox is a fullblock until it is hit sometimes, can be caused be restarting client and joining server
            if (block.getType() == StateTypes.BREWING_STAND && clientVersion.equals(ClientVersion.V_1_8) && Arrays.equals(vector3i, targetBlockVec)) {
                boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true));
            }

            currentEnd[0] = eyePos[0] + lookVec[0] * currentDistance;
            currentEnd[1] = eyePos[1] + lookVec[1] * currentDistance;
            currentEnd[2] = eyePos[2] + lookVec[2] * currentDistance;

            for (SimpleCollisionBox box : boxes) {
                Pair<double[], BlockFace> intercept = ReachUtilsPrimitives.calculateIntercept(box, eyePos, currentEnd);
                if (intercept.getFirst() == null) continue; // No intercept or wrong blockFace

                double[] hitLoc = intercept.getFirst();

                double distSq = distanceSquared(hitLoc, eyePos);
                if (distSq < bestHitResult) {
                    bestHitResult = distSq;
                    bestHitLoc = hitLoc;
                    bestFace = intercept.getSecond();
                }
            }

            if (bestHitLoc != null) {
                return new Pair<>(vector3i, bestFace);
            }

            return null;
        });
    }

    private static double distanceSquared(double[] vec1, double[] vec2) {
        double dx = vec1[0] - vec2[0];
        double dy = vec1[1] - vec2[1];
        double dz = vec1[2] - vec2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    public static HitData getNearestHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox) {
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        final double distance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        Vector endVec = trace.getPointAtDistance(distance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());

        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(distance));
                if (intercept.getFirst() == null) continue; // No intercept

                Vector hitLoc = intercept.getFirst();

                if (hitLoc.distanceSquared(startingVec) < bestHitResult) {
                    bestHitResult = hitLoc.distanceSquared(startingVec);
                    bestHitLoc = hitLoc;
                    bestFace = intercept.getSecond();
                }
            }
            if (bestHitLoc != null) {
                return new HitData(vector3i, bestHitLoc, bestFace, block);
            }

            if (sourcesHaveHitbox &&
                    (player.compensatedWorld.isWaterSourceBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ())
                            || player.compensatedWorld.getLavaFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ()) == (8 / 9f))) {
                double waterHeight = player.compensatedWorld.getFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
                SimpleCollisionBox box = new SimpleCollisionBox(vector3i.getX(), vector3i.getY(), vector3i.getZ(), vector3i.getX() + 1, vector3i.getY() + waterHeight, vector3i.getZ() + 1);

                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(distance));

                if (intercept.getFirst() != null) {
                    return new HitData(vector3i, intercept.getFirst(), intercept.getSecond(), block);
                }
            }

            return null;
        });
    }
}
