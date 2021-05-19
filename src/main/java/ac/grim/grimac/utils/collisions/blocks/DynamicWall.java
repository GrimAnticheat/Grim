package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("Duplicates")
public class DynamicWall implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016

    // Wall sides are different in 1.13 and reflect what they look like
    private static final double width = 0.25;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    private static boolean wallConnects(ClientVersion v, int currX, int currY, int currZ, int x, int y, int z) {

        return false;
        /*Block targetBlock = fenceBlock.getRelative(direction, 1);
        BlockState sTarget = targetBlock.getState();
        Material target = sTarget.getType();

        if (!isWall(target) && DynamicFence.isBlacklisted(target))
            return false;

        if (target.name().contains("STAIRS")) {
            if (v.isBelow(ProtocolVersion.V1_12)) return false;
            Stairs stairs = (Stairs) sTarget.getData();
            return stairs.getFacing() == direction;
        } else return isWall(target) || (target.isSolid() && !target.isTransparent());*/
    }

    private static boolean isWall(Material m) {
        return m.name().contains("WALL");
    }

    public CollisionBox fetch(ClientVersion version, byte b, int x, int y, int z) {
        boolean var3 = wallConnects(version, x, y, z, x, y, z - 1);
        boolean var4 = wallConnects(version, x, y, z, x, y, z + 1);
        boolean var5 = wallConnects(version, x, y, z, x - 1, y, z);
        boolean var6 = wallConnects(version, x, y, z, x + 1, y, z);

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

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return fetch(version, (byte) 0, x, y, z);
    }

    @Override
    public CollisionBox fetch(ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
