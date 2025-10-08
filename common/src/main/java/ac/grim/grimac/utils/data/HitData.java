package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

public record HitData(
        Vector3i position,
        Vector3dm blockHitLocation,
        BlockFace closestDirection,
        WrappedBlockState state
) {
    public Vector3d getRelativeBlockHitLocation() {
        return new Vector3d(blockHitLocation.getX() - position.getX(), blockHitLocation.getY() - position.getY(), blockHitLocation.getZ() - position.getZ());
    }
}
