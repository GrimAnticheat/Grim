package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class MainSupportingBlockPosFinder {
    public MainSupportingBlockData findMainSupportingBlockPos(GrimPlayer player, MainSupportingBlockData lastSupportingBlock, Vector3d lastMovement, SimpleCollisionBox maxPose, boolean isOnGround) {
        if (!isOnGround) {
            return new MainSupportingBlockData(null, false);
        }

        SimpleCollisionBox slightlyBelowPlayer = new SimpleCollisionBox(maxPose.minX, maxPose.minY - 1.0E-6D, maxPose.minZ, maxPose.maxX, maxPose.minY, maxPose.maxZ);

        Vector3i supportingBlock = findSupportingBlock(player, slightlyBelowPlayer);
        if (supportingBlock == null && (!lastSupportingBlock.lastOnGroundAndNoBlock())) {
            if (lastMovement != null) {
                SimpleCollisionBox aabb2 = slightlyBelowPlayer.offset(-lastMovement.x, 0.0D, -lastMovement.z);
                return new MainSupportingBlockData(findSupportingBlock(player, aabb2), true);
            }
        } else {
            return new MainSupportingBlockData(supportingBlock, true);
        }

        return new MainSupportingBlockData(null, true);
    }

    private static class BestBlockHolder {
        Vector3i pos;
        double distanceSquared = Double.MAX_VALUE;
    }

    // Yes we could make this way more efficient with bit packing with 0 object allocations
    // No I'm not making this code less readable unless we need to
    private @Nullable Vector3i findSupportingBlock(@NotNull GrimPlayer player, @NotNull SimpleCollisionBox searchBox) {
        final double playerX = player.x;
        final double playerY = player.y;
        final double playerZ = player.z;

        final BestBlockHolder bestBlock = new BestBlockHolder();

        Collisions.forEachCollisionBox(player, searchBox, (blockPosX, blockPosY, blockPosZ) -> {
            double distanceSquared = GrimMath.distanceSquared(playerX, playerY, playerZ, blockPosX + 0.5, blockPosY + 0.5, blockPosZ + 0.5);

            if (distanceSquared < bestBlock.distanceSquared || distanceSquared == bestBlock.distanceSquared && (bestBlock.pos == null || firstHasPriorityOverSecond(blockPosX, blockPosY, blockPosZ, bestBlock.pos))) {
                bestBlock.pos = new Vector3i(blockPosX, blockPosY, blockPosZ);
                bestBlock.distanceSquared = distanceSquared;
            }
        });

        return bestBlock.pos;
    }

    private boolean firstHasPriorityOverSecond(int x, int y, int z, @NotNull Vector3i second) {
        // Order of loop is X, Y, and Z
        // We prioritize lowest Y axis, then lowest X axis, then lowest Z axis
        // Ties among the X and Z positions are broken by the order of looping being X
        //
        // X O O
        // 0 X 0
        // 0 0 X
        // If the three blocks were this, the lowest right would win because of iteration order
        //
        // X 0 0
        // 0 0 X
        // But the upper left would win here because of prioritizing negative X and negative Z
        if (y < second.getY()) return true;

        double sumX = second.getX() - x;
        double sumY = second.getZ() - z;

        double horizontalSumTotal = sumX + sumY;
        if (horizontalSumTotal == 0) {
            // If X is farther in the X direction, then it was found later and therefore won't override
            return sumX < 0;
        }

        // Otherwise, lower X and lower Z have priority
        return horizontalSumTotal < 0;
    }
}
