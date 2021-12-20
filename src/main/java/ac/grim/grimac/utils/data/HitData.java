package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.util.Vector;

@Getter
@ToString
public class HitData {
    Vector3i position;
    Vector blockHitLocation;
    BaseBlockState state;
    BlockFace closestDirection;

    public HitData(Vector3i position, Vector blockHitLocation, BlockFace closestDirection, BaseBlockState state) {
        this.position = position;
        this.blockHitLocation = blockHitLocation;
        this.closestDirection = closestDirection;
        this.state = state;
    }

    public Vector3d getRelativeBlockHitLocation() {
        return new Vector3d(blockHitLocation.getX() - position.getX(), blockHitLocation.getY() - position.getY(), blockHitLocation.getZ() - position.getZ());
    }
}
