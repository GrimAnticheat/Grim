package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

@Getter
@ToString
public class HitData {
    Vector3i position;
    Vector blockHitLocation;
    BaseBlockState state;
    BlockFace closestDirection;

    public HitData(Vector3i position, Vector blockHitLocation, BaseBlockState state) {
        this.position = position;
        this.blockHitLocation = blockHitLocation;
        this.state = state;
        closestDirection = getNearest(blockHitLocation.getX(), blockHitLocation.getY(), blockHitLocation.getZ());
    }

    private BlockFace getNearest(double x, double y, double z) {
        return getNearest((float) x, (float) y, (float) z);
    }

    private BlockFace getNearest(float x, float y, float z) {
        BlockFace direction = BlockFace.NORTH;
        float f = Float.MIN_VALUE;

        for (BlockFace direction1 : BlockFace.values()) {
            if (!direction1.isCartesian()) continue;

            float f1 = x * direction1.getModX() + y * direction1.getModY() + z * direction1.getModZ();
            if (f1 > f) {
                f = f1;
                direction = direction1;
            }
        }

        return direction;
    }
}
