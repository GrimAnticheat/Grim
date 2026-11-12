package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.functions.ObjectAndPositionFunction;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class WorldRayTrace {
    public static HitData getNearestBlockHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox, boolean fluidPlacement, boolean itemUsePlacement) {
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Ray trace = Ray.from(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.yaw, player.pitch);
        final double distance = itemUsePlacement && player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5) ? 5 : player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vector3d endPos = trace.getPointAtDistance(distance);

        return traverseBlocks(player, startingPos, endPos, (block, x, y, z) -> {
            if (fluidPlacement && player.getClientVersion().isOlderThan(ClientVersion.V_1_13) && CollisionData.getData(block.getType())
                    .getMovementCollisionBox(player, player.getClientVersion(), block, x, y, z).isNull()) {
                return null;
            }

            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, false, x, y, z);
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector3dm bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                Pair<Vector3d, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace, distance);
                if (intercept.first() == null) continue; // No intercept

                double d = intercept.first().distanceSquared(startingPos);
                if (d < bestHitResult) {
                    bestHitResult = d;
                    bestHitLoc = Vector3dm.from(intercept.first());
                    bestFace = intercept.second();
                }
            }

            if (bestHitLoc != null) {
                return new HitData(new Vector3i(x, y, z), bestHitLoc, bestFace, block);
            }

            if (sourcesHaveHitbox &&
                    (player.compensatedWorld.isWaterSourceBlock(x, y, z)
                            || player.compensatedWorld.getLavaFluidLevelAt(x, y, z) == (8 / 9f))) {
                double waterHeight = player.getClientVersion().isOlderThan(ClientVersion.V_1_13) ? 1
                        : player.compensatedWorld.getFluidLevelAt(x, y, z);
                SimpleCollisionBox box = new SimpleCollisionBox(x, y, z, x + 1, y + waterHeight, z + 1);

                Pair<Vector3d, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace, distance);

                if (intercept.first() != null) {
                    return new HitData(new Vector3i(x, y, z), Vector3dm.from(intercept.first()), intercept.second(), block);
                }
            }

            return null;
        });
    }

    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    public static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, ObjectAndPositionFunction<WrappedBlockState, HitData> predicate) {
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

        WrappedBlockState state = player.compensatedWorld.getBlock(floorStartX, floorStartY, floorStartZ);
        HitData apply = predicate.apply(state, floorStartX, floorStartY, floorStartZ);

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

        double d12 = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double d13 = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double d14 = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // Can't figure out what this code does currently
        while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
            if (d12 < d13) {
                if (d12 < d14) {
                    floorStartX += xSign;
                    d12 += posXInverse;
                } else {
                    floorStartZ += zSign;
                    d14 += posZInverse;
                }
            } else if (d13 < d14) {
                floorStartY += ySign;
                d13 += posYInverse;
            } else {
                floorStartZ += zSign;
                d14 += posZInverse;
            }

            state = player.compensatedWorld.getBlock(floorStartX, floorStartY, floorStartZ);
            apply = predicate.apply(state, floorStartX, floorStartY, floorStartZ);

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }
}
