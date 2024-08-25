package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class BlockRayTrace {

    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    public static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end.x, start.x);
        double endY = GrimMath.lerp(-1.0E-7D, end.y, start.y);
        double endZ = GrimMath.lerp(-1.0E-7D, end.z, start.z);
        double startX = GrimMath.lerp(-1.0E-7D, start.x, end.x);
        double startY = GrimMath.lerp(-1.0E-7D, start.y, end.y);
        double startZ = GrimMath.lerp(-1.0E-7D, start.z, end.z);
        int floorStartX = GrimMath.floor(startX);
        int floorStartY = GrimMath.floor(startY);
        int floorStartZ = GrimMath.floor(startZ);


        if (start.equals(end)) return null;

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
        // The loop continues until the ray has crossed a block boundary along all axes
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

    public static HitData getNearestHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox) {
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        final double distance = player.compensatedEntities.getSelf().getAttributeValue(
            Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        Vector endVec = trace.getPointAtDistance(distance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());
        return getTraverseResult(player, heldItem, startingPos, startingVec, trace, endPos, sourcesHaveHitbox, false, distance + 3);
    }

    @Nullable
    public static HitData getNearestReachHitResult(GrimPlayer player, Vector eyePos, Vector lookVec, double currentDistance, double maxDistance) {
        Vector3d startingPos = new Vector3d(eyePos.getX(), eyePos.getY(), eyePos.getZ());
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(eyePos, lookVec);
        Vector endVec = trace.getPointAtDistance(maxDistance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());
        return getTraverseResult(player, null, startingPos, startingVec, trace, endPos, false, true, currentDistance);
    }

    // This overload is intended for the LineOfSightPlace check and is expected to return blocks
    // Its (mostly) 0.03/0.0002 compensated unlike the other trace functions
    @Nullable
    public static HitData getNearestReachHitResult(GrimPlayer player, Vector eyePos, Vector lookVec, double currentDistance, double maxDistance, Vector3i targetBlockVec) {
        Vector3d startingPos = new Vector3d(eyePos.getX(), eyePos.getY(), eyePos.getZ());
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(eyePos, lookVec);
        Vector endVec = trace.getPointAtDistance(maxDistance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());

        StateType heldItem = null;
//        boolean sourcesHaveHitbox = false;
        boolean checkInside = true;
        double knownDistance = currentDistance;

        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                // Expand hitbox for 0.03/0.0002
                if (vector3i.equals(targetBlockVec)) {
                    box.expand(player.getMovementThreshold());
                } else {
                    // TODO figure out a better way to shrink every SimpleCollisionBox that makes up the CollisionBox by 0.03/0.0002
                    // Such that every direction except faces where the sub-boxes are joined together

                    // Is this even neccessary? After extensive testing I've failed to false flag even without the line above
                    // This makes it possible to bypass the check and still open chests behind walls
                    // If you look at the edges of a block
                    // box.expand(player.getMovementThreshold() * -1);
                }


                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(knownDistance));
                if (intercept.getFirst() == null) continue; // No intercept

                Vector hitLoc = intercept.getFirst();

                // If inside a block, return empty result for reach check (don't bother checking this?)
                if (checkInside && ReachUtils.isVecInside(box, trace.getOrigin())) {
                    return null;
                }

                if (hitLoc.distanceSquared(startingVec) < bestHitResult) {
                    bestHitResult = hitLoc.distanceSquared(startingVec);
                    bestHitLoc = hitLoc;
                    bestFace = intercept.getSecond();
                }
            }

            if (bestHitLoc != null) {
                return new HitData(vector3i, bestHitLoc, bestFace, block);
            }

            return null;
        });
    }

    private static HitData getTraverseResult(GrimPlayer player, @Nullable StateType heldItem, Vector3d startingPos, Vector startingVec, Ray trace, Vector3d endPos, boolean sourcesHaveHitbox, boolean checkInside, double knownDistance) {
        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(knownDistance));
                if (intercept.getFirst() == null) continue; // No intercept

                Vector hitLoc = intercept.getFirst();

                // If inside a block, return empty result for reach check (don't bother checking this?)
                if (checkInside && ReachUtils.isVecInside(box, trace.getOrigin())) {
                    return null;
                }

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

                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(knownDistance));

                if (intercept.getFirst() != null) {
                    return new HitData(vector3i, intercept.getFirst(), intercept.getSecond(), block);
                }
            }

            return null;
        });
    }
}
