package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

@SuppressWarnings("Duplicates")
public class DynamicWall extends DynamicConnecting implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
        boolean south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
        boolean west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        boolean east = connectsTo(player, version, x, y, z, BlockFace.EAST);

        if (!XMaterial.isNewVersion() || version.isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
        } else {
        }

        double var7 = 0.25;
        double var8 = 0.75;
        double var9 = 0.25;
        double var10 = 0.75;

        if (north) {
            var9 = 0.0;
        }

        if (south) {
            var10 = 1.0;
        }

        if (west) {
            var7 = 0.0;
        }

        if (east) {
            var8 = 1.0;
        }

        if (north && south && !west && !east) {
            var7 = 0.3125;
            var8 = 0.6875;
        } else if (!north && !south && west && east) {
            var9 = 0.3125;
            var10 = 0.6875;
        }

        return new SimpleCollisionBox(var7, 0.0, var9, var8, 1.5, var10);
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        if (Materials.checkFlag(one, Materials.WALL))
            return true;
        else
            return CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
