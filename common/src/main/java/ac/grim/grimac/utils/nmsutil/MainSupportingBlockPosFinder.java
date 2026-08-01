package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

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
        Vector3d playerPos = new Vector3d(player.x, player.y, player.z);

        AtomicReference<Vector3i> bestBlockPos = new AtomicReference<>();
        AtomicDouble blockPosDistance = new AtomicDouble(Double.MAX_VALUE);

        Collisions.forEachCollisionBox(player, searchBox, (block, x, y, z) -> {
            Vector3d center = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
            double distance = playerPos.distanceSquared(center);

            if (distance < blockPosDistance.get() || distance == blockPosDistance.get() && (bestBlockPos.get() == null || firstHasPriorityOverSecond(x, y, z, bestBlockPos.get()))) {
                bestBlockPos.set(new Vector3i(x, y, z));
                blockPosDistance.set(distance);
            }
        });

        return bestBlockPos.get();
    }

    private boolean firstHasPriorityOverSecond(int firstX, int firstY, int firstZ, @NotNull Vector3i second) {
        // Vanilla (CollisionGetter#findSupportingBlock) keeps the candidate over the current best
        // when best.compareTo(candidate) < 0, and Vec3i#compareTo orders lexicographically by
        // Y, then Z, then X. So on a distance tie the greatest block in that order wins.
        //
        // This must be a strict lexicographic order, not a sum of the axis deltas: (0,y,5) vs
        // (5,y,0) sums to zero on both axes while the Z comparison clearly separates them.
        if (second.getY() != firstY) return second.getY() < firstY;
        if (second.getZ() != firstZ) return second.getZ() < firstZ;
        return second.getX() < firstX;
    }
}
