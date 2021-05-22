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
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class DynamicFence extends DynamicConnecting implements CollisionFactory {
    // TODO: 1.9-1.11 clients don't have BARRIER exemption
    // https://bugs.mojang.com/browse/MC-9565
    // TODO: 1.4-1.11 clients don't check for fence gate direction
    // https://bugs.mojang.com/browse/MC-94016

    private static final Material NETHER_BRICK_FENCE = XMaterial.NETHER_BRICK_FENCE.parseMaterial();
    private static final CollisionBox[] COLLISION_BOXES = makeShapes(2.0F, 2.0F, 16.0F, 0.0F, 24.0F);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean east;
        boolean north;
        boolean south;
        boolean west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (XMaterial.isNewVersion() && version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            WrappedMultipleFacing fence = (WrappedMultipleFacing) block;

            east = fence.getDirections().contains(BlockFace.EAST);
            north = fence.getDirections().contains(BlockFace.NORTH);
            south = fence.getDirections().contains(BlockFace.SOUTH);
            west = fence.getDirections().contains(BlockFace.WEST);
        } else {
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        }

        return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        if (Materials.checkFlag(one, Materials.FENCE))
            return !(one == NETHER_BRICK_FENCE) && !(two == NETHER_BRICK_FENCE);
        else
            return Materials.checkFlag(one, Materials.FENCE) || CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
