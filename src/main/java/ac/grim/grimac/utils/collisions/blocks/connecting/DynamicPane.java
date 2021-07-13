package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedMultipleFacing;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

@SuppressWarnings("Duplicates")
public class DynamicPane extends DynamicConnecting implements CollisionFactory {

    private static final CollisionBox[] COLLISION_BOXES = makeShapes(1.0F, 1.0F, 16.0F, 0.0F, 16.0F, true);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean east;
        boolean north;
        boolean south;
        boolean west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (XMaterial.isNewVersion() && version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            WrappedMultipleFacing pane = (WrappedMultipleFacing) block;

            east = pane.getDirections().contains(BlockFace.EAST);
            north = pane.getDirections().contains(BlockFace.NORTH);
            south = pane.getDirections().contains(BlockFace.SOUTH);
            west = pane.getDirections().contains(BlockFace.WEST);
        } else {
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        }

        // On 1.7 and 1.8 clients, and 1.13+ clients on 1.7 and 1.8 servers, the glass pane is + instead of |
        if (!north && !south && !east && !west && (version.isOlderThanOrEquals(ClientVersion.v_1_8) || (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_8) && version.isNewerThanOrEquals(ClientVersion.v_1_13)))) {
            north = south = east = west = true;
        }

        return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
    }


    @Override
    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        if (Materials.checkFlag(one, Materials.GLASS_PANE))
            return true;
        else
            return CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
