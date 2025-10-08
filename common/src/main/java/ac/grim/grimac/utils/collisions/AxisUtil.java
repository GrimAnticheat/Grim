package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class AxisUtil {
    // I couldn't figure out what Mojang was doing, but I think this looks nice
    // Bounding boxes just have to be put into the modification thing before into this to be for faces
    public static @NotNull SimpleCollisionBox combine(@NotNull SimpleCollisionBox base, @NotNull SimpleCollisionBox toMerge) {
        boolean insideX = toMerge.minX <= base.minX && toMerge.maxX >= base.maxX;
        boolean insideY = toMerge.minY <= base.minY && toMerge.maxY >= base.maxY;
        boolean insideZ = toMerge.minZ <= base.minZ && toMerge.maxZ >= base.maxZ;

        if (insideX && insideY && !insideZ) {
            return new SimpleCollisionBox(base.minX, base.maxY, Math.min(base.minZ, toMerge.minZ), base.minX, base.maxY, Math.max(base.maxZ, toMerge.maxZ));
        } else if (insideX && !insideY && insideZ) {
            return new SimpleCollisionBox(base.minX, Math.min(base.minY, toMerge.minY), base.minZ, base.maxX, Math.max(base.maxY, toMerge.maxY), base.maxZ);
        } else if (!insideX && insideY && insideZ) {
            return new SimpleCollisionBox(Math.min(base.minX, toMerge.maxX), base.minY, base.maxZ, Math.max(base.minX, toMerge.minX), base.minY, base.maxZ);
        }

        return base;
    }

    @Contract(pure = true)
    public static boolean isSameAxis(BlockFace one, BlockFace two) {
        return one == two || one == two.getOppositeFace();
    }
}
