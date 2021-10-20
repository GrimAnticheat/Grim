package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.helper.BlockStateHelper;
import ac.grim.grimac.utils.collisions.AxisSelect;
import ac.grim.grimac.utils.collisions.AxisUtil;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.ReachUtils;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockPlace {
    private static final BlockFace[] BY_2D = new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
    Vector3i blockPosition;
    Direction face;
    private static final Material SOUL_SAND = XMaterial.SOUL_SAND.parseMaterial();
    boolean isCancelled = false;
    private static final Material SNOW = XMaterial.SNOW.parseMaterial();
    private static final Material COMPOSTER = XMaterial.COMPOSTER.parseMaterial();

    public Vector3i getPlacedAgainstBlockLocation() {
        return blockPosition;
    }

    private static final Material LADDER = XMaterial.LADDER.parseMaterial();
    GrimPlayer player;
    Material material;

    public BlockPlace(GrimPlayer player, Vector3i blockPosition, Direction face, Material material) {
        this.player = player;
        this.blockPosition = blockPosition;
        this.face = face;
        this.material = material;
    }

    public WrappedBlockDataValue getPlacedAgainstData() {
        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(getPlacedAgainstBlockLocation());
        return WrappedBlockData.getMaterialData(player.compensatedWorld.getWrappedBlockStateAt(getPlacedAgainstBlockLocation())).getData(state);
    }

    public BlockData getExistingBlockBlockData() {
        return ((FlatBlockState) player.compensatedWorld.getWrappedBlockStateAt(getPlacedBlockPos())).getBlockData();
    }

    public Material getPlacedAgainstMaterial() {
        return player.compensatedWorld.getWrappedBlockStateAt(getPlacedAgainstBlockLocation()).getMaterial();
    }

    public BaseBlockState getBelowState() {
        Vector3i pos = getPlacedBlockPos();
        pos.setY(pos.getY() - 1);
        return player.compensatedWorld.getWrappedBlockStateAt(pos);
    }

    public BaseBlockState getAboveState() {
        Vector3i pos = getPlacedBlockPos();
        pos.setY(pos.getY() + 1);
        return player.compensatedWorld.getWrappedBlockStateAt(pos);
    }

    /**
     * Warning: This is only valid for 1.13+ blocks.  If the block exists on 1.12 or below,
     * use the more generic getDirectionalState method.
     *
     * @param facing The direction from the placed block pos to get the block for
     * @return The cast BaseBlockState
     */
    public FlatBlockState getDirectionalFlatState(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos.setX(pos.getX() + facing.getModX());
        pos.setY(pos.getY() + facing.getModY());
        pos.setZ(pos.getZ() + facing.getModZ());
        return (FlatBlockState) player.compensatedWorld.getWrappedBlockStateAt(pos);
    }

    public BaseBlockState getDirectionalState(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos.setX(pos.getX() + facing.getModX());
        pos.setY(pos.getY() + facing.getModY());
        pos.setZ(pos.getZ() + facing.getModZ());
        return player.compensatedWorld.getWrappedBlockStateAt(pos);
    }

    public boolean isFullFace(BlockFace relative) {
        BaseBlockState east = getDirectionalState(relative);

        BlockFace face = relative.getOppositeFace();

        WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(east);
        AxisSelect axis = AxisUtil.getAxis(face);

        CollisionBox box = CollisionData.getData(east.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), east);

        Material blockMaterial = east.getMaterial();

        if (Materials.checkFlag(blockMaterial, Materials.LEAVES)) {
            // Leaves can't support blocks
            return false;
        } else if (blockMaterial == SNOW) {
            WrappedSnow snow = (WrappedSnow) dataValue;
            return snow.getLayers() == 8;
        } else if (Materials.checkFlag(blockMaterial, Materials.STAIRS)) {
            WrappedStairs stairs = (WrappedStairs) dataValue;

            if (face == BlockFace.UP) {
                return stairs.getUpsideDown();
            }
            if (face == BlockFace.DOWN) {
                return !stairs.getUpsideDown();
            }

            return stairs.getDirection() == face;
        } else if (blockMaterial == COMPOSTER) { // Composters have solid faces except for on the top
            return face != BlockFace.UP;
        } else if (blockMaterial == SOUL_SAND) { // Soul sand is considered to be a full block when placing things
            return true;
        } else if (blockMaterial == LADDER) { // Yes, although it breaks immediately, you can place blocks on ladders
            WrappedDirectional ladder = (WrappedDirectional) dataValue;
            return ladder.getDirection().getOppositeFace() == face;
        } else if (Materials.checkFlag(blockMaterial, Materials.TRAPDOOR)) { // You can place blocks that need solid faces on trapdoors
            WrappedTrapdoor trapdoor = (WrappedTrapdoor) dataValue;
            return trapdoor.getDirection().getOppositeFace() == face && trapdoor.isOpen();
        } else if (Materials.checkFlag(blockMaterial, Materials.DOOR)) { // You can place blocks that need solid faces on doors
            CollisionData data = CollisionData.getData(blockMaterial);

            if (data.dynamic instanceof DoorHandler) {
                int x = getPlacedAgainstBlockLocation().getX();
                int y = getPlacedAgainstBlockLocation().getY();
                int z = getPlacedAgainstBlockLocation().getZ();
                BlockFace dir = ((DoorHandler) data.dynamic).fetchDirection(player, player.getClientVersion(), dataValue, x, y, z);
                return dir.getOppositeFace() == face;
            }
        }

        if (box instanceof SimpleCollisionBox) {
            return axis.modify((SimpleCollisionBox) box).isFullBlockNoCache();
        }

        // Not an explicit edge case and is complicated, so isn't a full face
        return false;
    }

    public boolean isFaceFullCenter(BlockFace facing) {
        BaseBlockState data = getDirectionalState(facing);
        CollisionBox box = CollisionData.getData(data.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), data);

        if (box.isNull()) return false;
        if (isFullFace(facing)) return true;
        if (Materials.checkFlag(data.getMaterial(), Materials.LEAVES)) return false;

        List<SimpleCollisionBox> collisions = new ArrayList<>();
        box.downCast(collisions);

        AxisSelect axis = AxisUtil.getAxis(facing.getOppositeFace());

        for (SimpleCollisionBox simpleBox : collisions) {
            simpleBox = axis.modify(simpleBox);
            if (simpleBox.minX <= 7 / 16d && simpleBox.maxX >= 7 / 16d
                    && simpleBox.minY <= 0 && simpleBox.maxY >= 10 / 16d
                    && simpleBox.minZ <= 7 / 16d && simpleBox.maxZ >= 9 / 16d) {
                return true;
            }
        }

        return false;
    }

    public boolean isBlockFaceOpen(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos.setX(pos.getX() + facing.getModX());
        pos.setY(pos.getY() + facing.getModY());
        pos.setZ(pos.getZ() + facing.getModZ());
        return Materials.checkFlag(player.compensatedWorld.getWrappedBlockStateAt(pos).getMaterial(), Materials.REPLACEABLE);
    }

    // I believe this is correct, although I'm using a method here just in case it's a tick off... I don't trust Mojang
    public boolean isSecondaryUse() {
        return player.packetStateData.isPacketSneaking;
    }

    public Material getBelowMaterial() {
        return getBelowState().getMaterial();
    }

    public Direction getDirection() {
        return face;
    }

    public BlockFace getBlockFace() {
        return BlockFace.valueOf(getDirection().name());
    }

    // Copied from vanilla nms
    public List<BlockFace> getNearestLookingDirections() {
        float f = player.yRot * ((float) Math.PI / 180F);
        float f1 = -player.xRot * ((float) Math.PI / 180F);
        float f2 = player.trigHandler.sin(f);
        float f3 = player.trigHandler.cos(f);
        float f4 = player.trigHandler.sin(f1);
        float f5 = player.trigHandler.cos(f1);
        boolean flag = f4 > 0.0F;
        boolean flag1 = f2 < 0.0F;
        boolean flag2 = f5 > 0.0F;
        float f6 = flag ? f4 : -f4;
        float f7 = flag1 ? -f2 : f2;
        float f8 = flag2 ? f5 : -f5;
        float f9 = f6 * f3;
        float f10 = f8 * f3;
        BlockFace direction = flag ? BlockFace.EAST : BlockFace.WEST;
        BlockFace direction1 = flag1 ? BlockFace.UP : BlockFace.DOWN;
        BlockFace direction2 = flag2 ? BlockFace.SOUTH : BlockFace.NORTH;
        if (f6 > f8) {
            if (f7 > f9) {
                return Arrays.asList(direction1, direction, direction2);
            } else {
                return f10 > f7 ? Arrays.asList(direction, direction2, direction1) : Arrays.asList(direction, direction1, direction2);
            }
        } else if (f7 > f10) {
            return Arrays.asList(direction1, direction2, direction);
        } else {
            return f9 > f7 ? Arrays.asList(direction2, direction, direction1) : Arrays.asList(direction2, direction1, direction);
        }
    }

    public BlockFace getNearestVerticalDirection() {
        return player.yRot < 0.0F ? BlockFace.UP : BlockFace.DOWN;
    }

    public boolean isFaceHorizontal() {
        Direction face = getDirection();
        return face == Direction.NORTH || face == Direction.EAST || face == Direction.SOUTH || face == Direction.WEST;
    }

    public boolean isFaceVertical() {
        return !isFaceHorizontal();
    }

    public boolean isXAxis() {
        Direction face = getDirection();
        return face == Direction.WEST || face == Direction.EAST;
    }

    public boolean isZAxis() {
        Direction face = getDirection();
        return face == Direction.NORTH || face == Direction.SOUTH;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public Vector3i getPlacedBlockPos() {
        int x = blockPosition.getX() + getNormalBlockFace().getX();
        int y = blockPosition.getY() + getNormalBlockFace().getY();
        int z = blockPosition.getZ() + getNormalBlockFace().getZ();
        return new Vector3i(x, y, z);
    }

    public Vector3i getNormalBlockFace() {
        switch (face) {
            default:
            case UP:
                return new Vector3i(0, 1, 0);
            case DOWN:
                return new Vector3i(0, -1, 0);
            case SOUTH:
                return new Vector3i(0, 0, 1);
            case NORTH:
                return new Vector3i(0, 0, -1);
            case WEST:
                return new Vector3i(-1, 0, 0);
            case EAST:
                return new Vector3i(1, 0, 0);
        }
    }

    public void set(Material material) {
        set(BlockStateHelper.create(material));
    }

    public void set(BlockFace face, BaseBlockState state) {
        Vector3i blockPos = getPlacedBlockPos();
        player.compensatedWorld.updateBlock(blockPos.getX() + face.getModX(), blockPos.getY() + face.getModY(),
                blockPos.getZ() + face.getModZ(), state.getCombinedId());
    }

    public void set(Vector3i position, BaseBlockState state) {
        player.compensatedWorld.updateBlock(position.getX(), position.getY(), position.getZ(), state.getCombinedId());
    }

    public boolean set(BlockData state) {
        // TODO: Check if the player is inside of the block - if so, ignore this place
        // TODO: We need to check if there is a full block of water and this blockdata is waterlogged
        set(new FlatBlockState(state));
        Bukkit.broadcastMessage("Placed " + state.getAsString(false));
        return true;
    }

    public void set(BaseBlockState state) {
        set(getPlacedBlockPos(), state);
    }

    public void resync() {
        isCancelled = true;
    }

    // All method with rants about mojang must go below this line

    // MOJANG??? Why did you remove this from the damn packet.  YOU DON'T DO BLOCK PLACING RIGHT!
    // You use last tick vector on the server and current tick on the client...
    // You also have 0.03 for FIVE YEARS which will mess this up.  nice one mojang
    // Fix your damn netcode
    //
    // You also have the desync caused by eye height as apparently tracking the player's ticks wasn't important to you
    // No mojang, you really do need to track client ticks to get their accurate eye height.
    // another damn desync added... maybe next decade it will get fixed and double the amount of issues.
    public Vector getClickedLocation() {
        SimpleCollisionBox box = new SimpleCollisionBox(getPlacedAgainstBlockLocation());
        Vector3d from = player.packetStateData.packetPosition;
        Vector look = ReachUtils.getLook(player, player.packetStateData.lastPacketPlayerXRot, player.packetStateData.lastPacketPlayerYRot);

        // TODO: Calculate actual eye height (which can also desync!)
        Vector eyePos = new Vector(from.getX(), from.getY() + 1.62, from.getZ());
        Vector endReachPos = eyePos.clone().add(new Vector(look.getX() * 6, look.getY() * 6, look.getZ() * 6));
        Vector intercept = ReachUtils.calculateIntercept(box, eyePos, endReachPos);

        // Bring this back to relative to the block
        // The player didn't even click the block... (we should force resync BEFORE we get here!)
        if (intercept == null) return new Vector();

        intercept.setX(intercept.getX() % 1);
        intercept.setY(intercept.getY() % 1);
        intercept.setZ(intercept.getZ() % 1);

        return intercept;
    }

    // This is wrong, we need next tick's look vector because mojang is shit at netcode...
    // FOR FUCKS SAKE MOJANG WHY DIDN'T YOU FIX THIS WHEN YOU "FIXED" THE BUCKET DESYNC!
    // Are you that incompetent???  Fix the root cause!
    public BlockFace getPlayerFacing() {
        return BY_2D[GrimMath.floor(player.packetStateData.packetPlayerXRot / 90.0D + 0.5D) & 3];
    }
}
