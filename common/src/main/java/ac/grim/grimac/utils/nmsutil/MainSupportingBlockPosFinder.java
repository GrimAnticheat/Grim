package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class MainSupportingBlockPosFinder {
    public @NotNull MainSupportingBlockData findMainSupportingBlockPos(GrimPlayer player, MainSupportingBlockData lastSupportingBlock, Vector3d lastMovement, SimpleCollisionBox maxPose, boolean isOnGround) {
        if (!isOnGround) {
            return MainSupportingBlockData.AIR_OFF_GROUND;
        }

        SimpleCollisionBox slightlyBelowPlayer = new SimpleCollisionBox(maxPose.minX, maxPose.minY - 1.0E-6D, maxPose.minZ, maxPose.maxX, maxPose.minY, maxPose.maxZ);

        Vector3i supportingBlock = findSupportingBlock(player, slightlyBelowPlayer);
        if (supportingBlock == null && !lastSupportingBlock.lastOnGroundAndNoBlock()) {
            if (lastMovement != null) {
                SimpleCollisionBox aabb2 = slightlyBelowPlayer.offset(-lastMovement.x, 0.0D, -lastMovement.z);
                return new MainSupportingBlockData(findSupportingBlock(player, aabb2), true);
            }
        } else {
            return new MainSupportingBlockData(supportingBlock, true);
        }

        return MainSupportingBlockData.AIR_ON_GROUND;
    }

    private @Nullable Vector3i findSupportingBlock(@NotNull GrimPlayer player, @NotNull SimpleCollisionBox searchBox) {
        double playerX = player.x;
        double playerY = player.y;
        double playerZ = player.z;

        class Data {
            private int x, y, z;
            private double distance = Double.POSITIVE_INFINITY;
        }

        Data data = new Data();

        Collisions.forEachCollisionBox(player, searchBox, (block, x, y, z) -> {
            double distance = VectorUtils.distanceSquared(playerX, playerY, playerZ, x + 0.5, y + 0.5, z + 0.5);

            if (distance < data.distance || distance == data.distance && (data.distance == Double.POSITIVE_INFINITY || firstHasPriorityOverSecond(x, y, z, data.x, data.y, data.z))) {
                data.x = x;
                data.y = y;
                data.z = z;
                data.distance = distance;
            }
        });

        return data.distance == Double.POSITIVE_INFINITY ? null : new Vector3i(data.x, data.y, data.z);
    }

    @Contract(pure = true)
    private boolean firstHasPriorityOverSecond(int firstX, int firstY, int firstZ, int secondX, int secondY, int secondZ) {
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
        if (firstY < secondY) return true;

        double sumX = secondX - firstX;
        double sumY = secondZ - firstZ;

        double horizontalSumTotal = sumX + sumY;
        if (horizontalSumTotal == 0) {
            // If X is farther in the X direction, then it was found later and therefore won't override
            return sumX < 0;
        }

        // Otherwise, lower X and lower Z have priority
        return horizontalSumTotal < 0;
    }
}
