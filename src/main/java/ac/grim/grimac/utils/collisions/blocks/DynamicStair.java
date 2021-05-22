package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedStairs;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;

public class DynamicStair implements CollisionFactory {
    private static EnumShape getStairsShape(GrimPlayer player, WrappedStairs originalStairs, int x, int y, int z) {
        BlockFace facing = originalStairs.getDirection();
        BaseBlockState offsetOne = player.compensatedWorld.getWrappedBlockStateAt(x + facing.getModX(), y + facing.getModY(), z + facing.getModZ());
        WrappedBlockDataValue iblockstate = WrappedBlockData.getMaterialData(offsetOne);

        if (Materials.checkFlag(offsetOne.getMaterial(), Materials.STAIRS) && originalStairs.getUpsideDown() == ((WrappedStairs) (iblockstate)).getUpsideDown()) {
            BlockFace enumfacing1 = ((WrappedStairs) (iblockstate)).getDirection();

            if (isDifferentAxis(facing, enumfacing1) && canTakeShape(player, originalStairs, x + enumfacing1.getOppositeFace().getModX(), y + enumfacing1.getOppositeFace().getModY(), z + enumfacing1.getOppositeFace().getModZ())) {
                if (enumfacing1 == rotateYCCW(facing)) {
                    return EnumShape.OUTER_LEFT;
                }

                return EnumShape.OUTER_RIGHT;
            }
        }

        BaseBlockState offsetTwo = player.compensatedWorld.getWrappedBlockStateAt(x + facing.getOppositeFace().getModX(), y + facing.getOppositeFace().getModY(), z + facing.getOppositeFace().getModZ());
        WrappedBlockDataValue iblockstate1 = WrappedBlockData.getMaterialData(offsetTwo);

        if (Materials.checkFlag(offsetTwo.getMaterial(), Materials.STAIRS) && originalStairs.getUpsideDown() == ((WrappedStairs) (iblockstate1)).getUpsideDown()) {
            BlockFace enumfacing2 = ((WrappedStairs) (iblockstate1)).getDirection();

            if (isDifferentAxis(facing, enumfacing2) && canTakeShape(player, originalStairs, x + enumfacing2.getModX(), y + enumfacing2.getModY(), z + enumfacing2.getModZ())) {
                if (enumfacing2 == rotateYCCW(facing)) {
                    return EnumShape.INNER_LEFT;
                }

                return EnumShape.INNER_RIGHT;
            }
        }

        return EnumShape.STRAIGHT;
    }

    private static boolean canTakeShape(GrimPlayer player, WrappedStairs stairOne, int x, int y, int z) {
        WrappedBlockDataValue otherStair = WrappedBlockData.getMaterialData(player.compensatedWorld.getWrappedBlockStateAt(x, y, z));
        return !(otherStair instanceof WrappedStairs) || (stairOne.getDirection() != ((WrappedStairs) otherStair).getDirection() || stairOne.getUpsideDown() != ((WrappedStairs) otherStair).getUpsideDown());
    }

    private static boolean isDifferentAxis(BlockFace faceOne, BlockFace faceTwo) {
        return faceOne.getOppositeFace() != faceTwo && faceOne != faceTwo;
    }

    private static BlockFace rotateYCCW(BlockFace face) {
        switch (face) {
            default:
            case NORTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
        }
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        Bukkit.broadcastMessage("Stair shape " + getStairsShape(player, (WrappedStairs) block, x, y, z));
        return NoCollisionBox.INSTANCE;
    }

    enum EnumShape {
        STRAIGHT,
        INNER_LEFT,
        INNER_RIGHT,
        OUTER_LEFT,
        OUTER_RIGHT
    }
}
