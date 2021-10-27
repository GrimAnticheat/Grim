package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedStairs;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.BlockFace;

import java.util.stream.IntStream;

public class DynamicStair implements CollisionFactory {
    protected static final CollisionBox TOP_AABB = new HexCollisionBox(0, 8, 0, 16, 16, 16);
    protected static final CollisionBox BOTTOM_AABB = new HexCollisionBox(0, 0, 0, 16, 8, 16);
    protected static final CollisionBox OCTET_NNN = new HexCollisionBox(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D);
    protected static final CollisionBox OCTET_NNP = new HexCollisionBox(0.0D, 0.0D, 8.0D, 8.0D, 8.0D, 16.0D);
    protected static final CollisionBox OCTET_NPN = new HexCollisionBox(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D);
    protected static final CollisionBox OCTET_NPP = new HexCollisionBox(0.0D, 8.0D, 8.0D, 8.0D, 16.0D, 16.0D);
    protected static final CollisionBox OCTET_PNN = new HexCollisionBox(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D);
    protected static final CollisionBox OCTET_PNP = new HexCollisionBox(8.0D, 0.0D, 8.0D, 16.0D, 8.0D, 16.0D);
    protected static final CollisionBox OCTET_PPN = new HexCollisionBox(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    protected static final CollisionBox OCTET_PPP = new HexCollisionBox(8.0D, 8.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox[] TOP_SHAPES = makeShapes(TOP_AABB, OCTET_NNN, OCTET_PNN, OCTET_NNP, OCTET_PNP);
    protected static final CollisionBox[] BOTTOM_SHAPES = makeShapes(BOTTOM_AABB, OCTET_NPN, OCTET_PPN, OCTET_NPP, OCTET_PPP);
    private static final int[] SHAPE_BY_STATE = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    public static EnumShape getStairsShape(GrimPlayer player, WrappedStairs originalStairs, int x, int y, int z) {
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

    private static CollisionBox[] makeShapes(CollisionBox p_199779_0_, CollisionBox p_199779_1_, CollisionBox p_199779_2_, CollisionBox p_199779_3_, CollisionBox p_199779_4_) {
        return IntStream.range(0, 16).mapToObj((p_199780_5_) -> makeStairShape(p_199780_5_, p_199779_0_, p_199779_1_, p_199779_2_, p_199779_3_, p_199779_4_)).toArray(CollisionBox[]::new);
    }

    private static CollisionBox makeStairShape(int p_199781_0_, CollisionBox p_199781_1_, CollisionBox p_199781_2_, CollisionBox p_199781_3_, CollisionBox p_199781_4_, CollisionBox p_199781_5_) {
        ComplexCollisionBox voxelshape = new ComplexCollisionBox(p_199781_1_);
        if ((p_199781_0_ & 1) != 0) {
            voxelshape.add(p_199781_2_);
        }

        if ((p_199781_0_ & 2) != 0) {
            voxelshape.add(p_199781_3_);
        }

        if ((p_199781_0_ & 4) != 0) {
            voxelshape.add(p_199781_4_);
        }

        if ((p_199781_0_ & 8) != 0) {
            voxelshape.add(p_199781_5_);
        }

        return voxelshape;
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedStairs stairs = (WrappedStairs) block;

        // If server is 1.13+ and client is also 1.13+, we can read the block's data directly
        if (stairs.getShapeOrdinal() != -1 && version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            return (stairs.getUpsideDown() ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_BY_STATE[getShapeIndex(stairs, stairs.getShapeOrdinal())]].copy();
        } else {
            // We need to read the world to determine the stair's block shape for:
            // 1.13 clients on 1.12 servers
            // 1.12 clients on 1.13 servers
            // 1.12 clients on 1.12 servers
            EnumShape shape = getStairsShape(player, stairs, x, y, z);
            return (stairs.getUpsideDown() ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_BY_STATE[getShapeIndex(stairs, shape.ordinal())]].copy();
        }
    }

    private int getShapeIndex(WrappedStairs p_196511_1_, int shapeOrdinal) {
        return shapeOrdinal * 4 + directionToValue(p_196511_1_.getDirection());
    }

    private int directionToValue(BlockFace face) {
        switch (face) {
            default:
            case UP:
            case DOWN:
                return -1;
            case NORTH:
                return 2;
            case SOUTH:
                return 0;
            case WEST:
                return 1;
            case EAST:
                return 3;
        }
    }

    enum EnumShape {
        STRAIGHT,
        INNER_LEFT,
        INNER_RIGHT,
        OUTER_LEFT,
        OUTER_RIGHT
    }
}
