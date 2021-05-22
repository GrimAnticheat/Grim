package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("Duplicates")
public class DynamicWall implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016

    private static boolean wallConnects(ClientVersion v, int currX, int currY, int currZ, int x, int y, int z) {

        return false;
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
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
