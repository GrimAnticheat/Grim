package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Shape;

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

    public static EnumShape getStairsShape(GrimPlayer player, WrappedBlockState originalStairs, int x, int y, int z) {
        BlockFace facing = originalStairs.getFacing();
        WrappedBlockState offsetOne = player.compensatedWorld.getWrappedBlockStateAt(x + facing.getModX(), y + facing.getModY(), z + facing.getModZ());

        if (Materials.isStairs(offsetOne.getType()) && originalStairs.getHalf() == offsetOne.getHalf()) {
            BlockFace enumfacing1 = offsetOne.getFacing();

            if (isDifferentAxis(facing, enumfacing1) && canTakeShape(player, originalStairs, x + enumfacing1.getOppositeFace().getModX(), y + enumfacing1.getOppositeFace().getModY(), z + enumfacing1.getOppositeFace().getModZ())) {
                if (enumfacing1 == rotateYCCW(facing)) {
                    return EnumShape.OUTER_LEFT;
                }

                return EnumShape.OUTER_RIGHT;
            }
        }

        WrappedBlockState offsetTwo = player.compensatedWorld.getWrappedBlockStateAt(x + facing.getOppositeFace().getModX(), y + facing.getOppositeFace().getModY(), z + facing.getOppositeFace().getModZ());

        if (Materials.isStairs(offsetTwo.getType()) && originalStairs.getHalf() == offsetTwo.getHalf()) {
            BlockFace enumfacing2 = offsetTwo.getFacing();

            if (isDifferentAxis(facing, enumfacing2) && canTakeShape(player, originalStairs, x + enumfacing2.getModX(), y + enumfacing2.getModY(), z + enumfacing2.getModZ())) {
                if (enumfacing2 == rotateYCCW(facing)) {
                    return EnumShape.INNER_LEFT;
                }

                return EnumShape.INNER_RIGHT;
            }
        }

        return EnumShape.STRAIGHT;
    }

    private static boolean canTakeShape(GrimPlayer player, WrappedBlockState stairOne, int x, int y, int z) {
        WrappedBlockState otherStair = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        return !(BlockTags.STAIRS.contains(otherStair.getType())) ||
                (stairOne.getFacing() != otherStair.getFacing() ||
                        stairOne.getHalf() != otherStair.getHalf());
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
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        int shapeOrdinal;
        // If server is 1.13+ and client is also 1.13+, we can read the block's data directly
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            shapeOrdinal = toEnumShape(block.getShape()).ordinal();
        } else {
            EnumShape shape = getStairsShape(player, block, x, y, z);
            shapeOrdinal = shape.ordinal();
        }
        return (block.getHalf() == Half.BOTTOM ? BOTTOM_SHAPES : TOP_SHAPES)[SHAPE_BY_STATE[getShapeIndex(block, shapeOrdinal)]].copy();
    }

    private int getShapeIndex(WrappedBlockState state, int shapeOrdinal) {
        return shapeOrdinal * 4 + directionToValue(state.getFacing());
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

    private EnumShape toEnumShape(Shape shape) {
        switch (shape) {
            default:
            case STRAIGHT:
                return EnumShape.STRAIGHT;
            case INNER_LEFT:
                return EnumShape.INNER_LEFT;
            case INNER_RIGHT:
                return EnumShape.INNER_RIGHT;
            case OUTER_LEFT:
                return EnumShape.OUTER_LEFT;
            case OUTER_RIGHT:
                return EnumShape.OUTER_RIGHT;
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
