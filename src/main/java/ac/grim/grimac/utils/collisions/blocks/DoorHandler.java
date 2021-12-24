package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedDoor;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.Material;
import BlockFace;

public class DoorHandler implements CollisionFactory {
    protected static final CollisionBox SOUTH_AABB = new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final CollisionBox NORTH_AABB = new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox WEST_AABB = new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox EAST_AABB = new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        switch (fetchDirection(player, version, block, x, y, z)) {
            case NORTH:
                return NORTH_AABB.copy();
            case SOUTH:
                return SOUTH_AABB.copy();
            case EAST:
                return EAST_AABB.copy();
            case WEST:
                return WEST_AABB.copy();
        }

        return NoCollisionBox.INSTANCE;
    }

    public BlockFace fetchDirection(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedDoor door = (WrappedDoor) block;
        Material doorMaterial = player.compensatedWorld.getBukkitMaterialAt(x, y, z);

        // 1.12 stores block data for the top door in the bottom block data
        // ViaVersion can't send 1.12 clients the 1.13 complete data
        // For 1.13, ViaVersion should just use the 1.12 block data
        if (!ItemTypes.isNewVersion() || version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            if (door.isBottom()) {
                BaseBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y + 1, z);

                // Doors have to be the same material in 1.12 for their block data to be connected together
                // For example, if you somehow manage to get a jungle top with an oak bottom, the data isn't shared
                WrappedBlockDataValue upperDoor = WrappedBlockData.getMaterialData(data);
                if (data.getMaterial() == doorMaterial && upperDoor instanceof WrappedDoor) {
                    door.setRightHinge(((WrappedDoor) upperDoor).isRightHinge());
                } else {
                    // Default missing value
                    door.setRightHinge(false);
                }
            } else {
                BaseBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y - 1, z);

                WrappedBlockDataValue lowerDoor = WrappedBlockData.getMaterialData(data);
                if (data.getMaterial() == doorMaterial && lowerDoor instanceof WrappedDoor) {
                    door.setOpen(((WrappedDoor) lowerDoor).getOpen());
                    door.setDirection(((WrappedDoor) lowerDoor).getDirection());
                } else {
                    door.setDirection(BlockFace.EAST);
                    door.setOpen(false);
                    door.setRightHinge(false);
                }
            }
        }

        BlockFace direction = door.getDirection();
        boolean flag = !door.getOpen();
        boolean flag1 = door.isRightHinge();
        switch (direction) {
            case EAST:
            default:
                return flag ? BlockFace.EAST : (flag1 ? BlockFace.NORTH : BlockFace.SOUTH);
            case SOUTH:
                return flag ? BlockFace.SOUTH : (flag1 ? BlockFace.EAST : BlockFace.WEST);
            case WEST:
                return flag ? BlockFace.WEST : (flag1 ? BlockFace.SOUTH : BlockFace.NORTH);
            case NORTH:
                return flag ? BlockFace.NORTH : (flag1 ? BlockFace.WEST : BlockFace.EAST);
        }
    }
}
