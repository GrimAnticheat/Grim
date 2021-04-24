package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Stairs;

@SuppressWarnings("Duplicates")
public class DynamicWall implements CollisionFactory {

    private static final double width = 0.25;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    private static boolean wallConnects(ProtocolVersion v, Block fenceBlock, BlockFace direction) {
        Block targetBlock = fenceBlock.getRelative(direction, 1);
        BlockState sTarget = targetBlock.getState();
        Material target = sTarget.getType();

        if (!isWall(target) && DynamicFence.isBlacklisted(target))
            return false;

        if (target.name().contains("STAIRS")) {
            if (v.isBelow(ProtocolVersion.V1_12)) return false;
            Stairs stairs = (Stairs) sTarget.getData();
            return stairs.getFacing() == direction;
        } else return isWall(target) || (target.isSolid() && !target.isTransparent());
    }

    private static boolean isWall(Material m) {
        return m.name().contains("WALL");
    }

    @Override
    public CollisionBox fetch(ProtocolVersion version, Block b) {
        boolean var3 = wallConnects(version, b, BlockFace.NORTH);
        boolean var4 = wallConnects(version, b, BlockFace.SOUTH);
        boolean var5 = wallConnects(version, b, BlockFace.WEST);
        boolean var6 = wallConnects(version, b, BlockFace.EAST);

        double var7 = 0.25;
        double var8 = 0.75;
        double var9 = 0.25;
        double var10 = 0.75;

        if (var3) {
            var9 = 0.0;
        }

        if (var4) {
            var10 = 1.0;
        }

        if (var5) {
            var7 = 0.0;
        }

        if (var6) {
            var8 = 1.0;
        }

        if (var3 && var4 && !var5 && !var6) {
            var7 = 0.3125;
            var8 = 0.6875;
        } else if (!var3 && !var4 && var5 && var6) {
            var9 = 0.3125;
            var10 = 0.6875;
        }

        return new SimpleCollisionBox(var7, 0.0, var9, var8, 1.5, var10);
    }

}
