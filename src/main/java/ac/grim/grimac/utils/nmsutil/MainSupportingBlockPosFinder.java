package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@UtilityClass
public class MainSupportingBlockPosFinder {
    public MainSupportingBlockData findMainSupportingBlockPos(GrimPlayer player, MainSupportingBlockData lastSupportingBlock, Vector3d lastMovement, SimpleCollisionBox maxPose, boolean isOnGround) {
        if (!isOnGround) {
            return new MainSupportingBlockData(null, false);
        }

        SimpleCollisionBox slightlyBelowPlayer = new SimpleCollisionBox(maxPose.minX, maxPose.minY - 1.0E-6D, maxPose.minZ, maxPose.maxX, maxPose.minY, maxPose.maxZ);

        Optional<Vector3i> supportingBlock = findSupportingBlock(player, slightlyBelowPlayer);
        if (!supportingBlock.isPresent() && (!lastSupportingBlock.lastOnGroundAndNoBlock())) {
            if (lastMovement != null) {
                SimpleCollisionBox aabb2 = slightlyBelowPlayer.offset(-lastMovement.x, 0.0D, -lastMovement.z);
                supportingBlock = findSupportingBlock(player, aabb2);
                return new MainSupportingBlockData(supportingBlock.orElse(null), true);
            }
        } else {
            return new MainSupportingBlockData(supportingBlock.orElse(null), true);
        }

        return new MainSupportingBlockData(null, true);
    }

    private Optional<Vector3i> findSupportingBlock(GrimPlayer player, SimpleCollisionBox searchBox) {
        Vector3d playerPos = new Vector3d(player.x, player.y, player.z);

        AtomicReference<Vector3i> bestBlockPos = new AtomicReference<>();
        AtomicDouble blockPosDistance = new AtomicDouble(Double.MAX_VALUE);

        Collisions.hasMaterial(player, searchBox, (thing) -> {
            Vector3i blockPos = thing.getSecond().toVector3i();

            CollisionBox collision = CollisionData.getData(thing.getFirst().getType()).getMovementCollisionBox(player, player.getClientVersion(), thing.getFirst(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!collision.isIntersected(searchBox)) return false;

            Vector3d blockPosAsVector3d = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            double distance = playerPos.distanceSquared(blockPosAsVector3d);

            if (distance < blockPosDistance.get() || distance == blockPosDistance.get() && (bestBlockPos.get() == null || firstHasPriorityOverSecond(blockPos, bestBlockPos.get()))) {
                bestBlockPos.set(blockPos);
                blockPosDistance.set(distance);
            }

            return false;
        });


        return Optional.ofNullable(bestBlockPos.get());
    }

    private boolean firstHasPriorityOverSecond(Vector3i first, Vector3i second) {
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
        if (first.getY() < second.getY()) return true;

        double sumX = second.getX() - first.getX();
        double sumY = second.getZ() - first.getZ();

        double horizontalSumTotal = sumX + sumY;
        if (horizontalSumTotal == 0) {
            // If X is farther in the X direction, then it was found later and therefore won't override
            return sumX < 0;
        }

        // Otherwise, lower X and lower Z have priority
        return horizontalSumTotal < 0;
    }
}
