package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedMultipleFacing;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class DynamicWall extends DynamicConnecting implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016
    private static final CollisionBox[] COLLISION_BOXES = makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, false);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean north;
        boolean south;
        boolean west;
        boolean east;
        boolean up;

        if (XMaterial.isNewVersion() || version.isNewerThan(ClientVersion.v_1_12_2)) {
            WrappedMultipleFacing pane = (WrappedMultipleFacing) block;

            east = pane.getDirections().contains(BlockFace.EAST);
            north = pane.getDirections().contains(BlockFace.NORTH);
            south = pane.getDirections().contains(BlockFace.SOUTH);
            west = pane.getDirections().contains(BlockFace.WEST);
            up = pane.getDirections().contains(BlockFace.UP);
        } else {
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            up = true;
        }

        // Proper and faster way would be to compute all this beforehand
        if (up) {
            ComplexCollisionBox box = new ComplexCollisionBox(COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy());
            box.add(new HexCollisionBox(4, 0, 4, 12, 24, 12));
            return box;
        }

        return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        return Materials.checkFlag(one, Materials.WALL) || CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
