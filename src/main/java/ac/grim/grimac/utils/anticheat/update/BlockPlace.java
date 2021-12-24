package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.helper.BlockStateHelper;
import ac.grim.grimac.utils.collisions.AxisSelect;
import ac.grim.grimac.utils.collisions.AxisUtil;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.GlowLichen;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockPlace {
    private static final BlockFace[] BY_2D = new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
    @Setter
    Vector3i blockPosition;
    protected static final BlockFace[] UPDATE_SHAPE_ORDER = new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP};
    @Getter
    @Setter
    boolean replaceClicked;
    private static final Material SOUL_SAND = ItemTypes.SOUL_SAND;
    boolean isCancelled = false;
    private static final Material SNOW = ItemTypes.SNOW;
    private static final Material COMPOSTER = ItemTypes.COMPOSTER;

    public Vector3i getPlacedAgainstBlockLocation() {
        return blockPosition;
    }

    private static final Material LADDER = ItemTypes.LADDER;
    GrimPlayer player;
    @Getter
    Material material;
    @Getter
    HitData hitData;
    @Setter
    BlockFace face;

    public BlockPlace(GrimPlayer player, Vector3i blockPosition, BlockFace face, Material material, HitData hitData) {
        this.player = player;
        this.blockPosition = blockPosition;
        this.face = face;
        this.material = material;
        this.hitData = hitData;

        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(getPlacedAgainstBlockLocation());
        WrappedBlockDataValue placedAgainst = WrappedBlockData.getMaterialData(state).getData(state);
        this.replaceClicked = canBeReplaced(material, state, placedAgainst);
    }

    public WrappedBlockDataValue getExistingBlockData() {
        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(getPlacedBlockPos());
        return WrappedBlockData.getMaterialData(state).getData(state);
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

    public boolean isSolid(BlockFace relative) {
        BaseBlockState state = getDirectionalState(relative);
        return !Materials.checkFlag(state.getMaterial(), Materials.SOLID_BLACKLIST);
    }

    private boolean canBeReplaced(Material heldItem, BaseBlockState state, WrappedBlockDataValue placedAgainst) {
        // Cave vines and weeping vines have a special case... that always returns false (just like the base case for it!)
        boolean baseReplaceable = state.getMaterial() != heldItem && Materials.checkFlag(state.getMaterial(), Materials.REPLACEABLE); // TODO: Buckets correctly!

        if (state.getMaterial().name().endsWith("CANDLE")) {
            Candle candle = (Candle) ((FlatBlockState) state).getBlockData();
            return heldItem == state.getMaterial() && candle.getCandles() < 4 && !isSecondaryUse();
        }
        if (state.getMaterial() == Material.SEA_PICKLE) {
            SeaPickle pickle = (SeaPickle) ((FlatBlockState) state).getBlockData();
            return heldItem == pickle.getMaterial() && pickle.getPickles() < 4 && !isSecondaryUse();
        }
        if (state.getMaterial() == Material.TURTLE_EGG) {
            TurtleEgg egg = (TurtleEgg) ((FlatBlockState) state).getBlockData();
            return heldItem == egg.getMaterial() && egg.getEggs() < 4 && !isSecondaryUse();
        }
        if (state.getMaterial() == Material.GLOW_LICHEN) {
            GlowLichen lichen = (GlowLichen) ((FlatBlockState) state).getBlockData();
            return lichen.getFaces().size() < lichen.getAllowedFaces().size() || heldItem != Material.GLOW_LICHEN;
        }
        if (state.getMaterial() == Material.SCAFFOLDING) {
            return heldItem == Material.SCAFFOLDING;
        }
        if (placedAgainst instanceof WrappedSlab) {
            WrappedSlab slab = (WrappedSlab) placedAgainst;
            if (slab.isDouble() || state.getMaterial() != heldItem) return false;

            // Here vanilla refers from
            // Set check can replace -> get block -> call block canBeReplaced -> check can replace boolean (default true)
            // uh... what?  I'm unsure what Mojang is doing here.  I think they just made a stupid mistake.
            // as this code is quite old.
            boolean flag = getClickedLocation().getY() > 0.5D;
            BlockFace clickedFace = getDirection();
            if (slab.isBottom()) {
                return clickedFace == BlockFace.UP || flag && isFaceHorizontal();
            } else {
                return clickedFace == BlockFace.DOWN || !flag && isFaceHorizontal();
            }
        }
        if (placedAgainst instanceof WrappedSnow) {
            int layers = ((WrappedSnow) placedAgainst).getLayers();
            if (heldItem == state.getMaterial() && layers < 7) { // We index at 0 (less than 8 layers)
                return true;
            } else {
                return layers == 0; // index at 0, (1 layer)
            }
        }
        if (state.getMaterial() == Material.VINE) {
            return baseReplaceable || (heldItem == state.getMaterial() && ((WrappedMultipleFacing) placedAgainst).getDirections().size() < 5); // up, north, east, west, south
        }

        return baseReplaceable;
    }

    public boolean isFaceFullCenter(BlockFace facing) {
        BaseBlockState data = getDirectionalState(facing);
        CollisionBox box = CollisionData.getData(data.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), data);

        if (box.isNull()) return false;
        if (isFullFace(facing)) return true;
        if (Materials.checkFlag(data.getMaterial(), Materials.LEAVES)) return false;
        if (Materials.checkFlag(data.getMaterial(), Materials.GATE)) return false;

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

    public boolean isFaceRigid(BlockFace facing) {
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
            if (simpleBox.minX <= 2 / 16d && simpleBox.maxX >= 14 / 16d
                    && simpleBox.minY <= 0 && simpleBox.maxY >= 1
                    && simpleBox.minZ <= 2 / 16d && simpleBox.maxZ >= 14 / 16d) {
                return true;
            }
        }

        return false;
    }

    public boolean isFullFace(BlockFace relative) {
        BaseBlockState state = getDirectionalState(relative);
        BlockFace face = relative.getOppositeFace();
        BlockFace bukkitFace = BlockFace.valueOf(face.name());

        WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(state);
        AxisSelect axis = AxisUtil.getAxis(face);

        CollisionBox box = CollisionData.getData(state.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), state);

        Material blockMaterial = state.getMaterial();

        if (Materials.checkFlag(blockMaterial, Materials.LEAVES)) {
            // Leaves can't support blocks
            return false;
        } else if (blockMaterial == SNOW) {
            WrappedSnow snow = (WrappedSnow) dataValue;
            return snow.getLayers() == 7;
        } else if (Materials.checkFlag(blockMaterial, Materials.STAIRS)) {
            WrappedStairs stairs = (WrappedStairs) dataValue;

            if (face == BlockFace.UP) {
                return stairs.getUpsideDown();
            }
            if (face == BlockFace.DOWN) {
                return !stairs.getUpsideDown();
            }


            return stairs.getDirection() == bukkitFace;
        } else if (blockMaterial == COMPOSTER) { // Composters have solid faces except for on the top
            return face != BlockFace.UP;
        } else if (blockMaterial == SOUL_SAND) { // Soul sand is considered to be a full block when placing things
            return true;
        } else if (blockMaterial == LADDER) { // Yes, although it breaks immediately, you can place blocks on ladders
            WrappedDirectional ladder = (WrappedDirectional) dataValue;
            return ladder.getDirection().getOppositeFace() == bukkitFace;
        } else if (Materials.checkFlag(blockMaterial, Materials.TRAPDOOR)) { // You can place blocks that need solid faces on trapdoors
            WrappedTrapdoor trapdoor = (WrappedTrapdoor) dataValue;
            return trapdoor.getDirection().getOppositeFace() == bukkitFace && trapdoor.isOpen();
        } else if (Materials.checkFlag(blockMaterial, Materials.DOOR)) { // You can place blocks that need solid faces on doors
            CollisionData data = CollisionData.getData(blockMaterial);

            if (data.dynamic instanceof DoorHandler) {
                int x = getPlacedAgainstBlockLocation().getX();
                int y = getPlacedAgainstBlockLocation().getY();
                int z = getPlacedAgainstBlockLocation().getZ();
                BlockFace dir = ((DoorHandler) data.dynamic).fetchDirection(player, player.getClientVersion(), dataValue, x, y, z);
                return dir.getOppositeFace() == bukkitFace;
            }
        }

        List<SimpleCollisionBox> collisions = new ArrayList<>();
        box.downCast(collisions);

        for (SimpleCollisionBox simpleBox : collisions) {
            if (axis.modify(simpleBox).isFullBlockNoCache()) return true;
        }

        // Not an explicit edge case and is complicated, so isn't a full face
        return false;
    }

    public boolean isBlockFaceOpen(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos.setX(pos.getX() + facing.getModX());
        pos.setY(pos.getY() + facing.getModY());
        pos.setZ(pos.getZ() + facing.getModZ());
        // You can't build above height limit.
        if (pos.getY() >= player.compensatedWorld.getMaxHeight()) return false;

        return Materials.checkFlag(player.compensatedWorld.getWrappedBlockStateAt(pos).getMaterial(), Materials.REPLACEABLE);
    }


    public boolean isFaceEmpty(BlockFace facing) {
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
            // If all sides to the box have width, there is collision.
            switch (facing) {
                case NORTH:
                    if (simpleBox.minZ == 0) return false;
                    break;
                case SOUTH:
                    if (simpleBox.maxZ == 1) return false;
                    break;
                case EAST:
                    if (simpleBox.maxX == 1) return false;
                    break;
                case WEST:
                    if (simpleBox.minX == 0) return false;
                    break;
                case UP:
                    if (simpleBox.maxY == 1) return false;
                    break;
                case DOWN:
                    if (simpleBox.minY == 0) return false;
                    break;
            }
        }

        return true;
    }

    public boolean isLava(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos.setX(pos.getX() + facing.getModX());
        pos.setY(pos.getY() + facing.getModY());
        pos.setZ(pos.getZ() + facing.getModZ());
        return Materials.checkFlag(player.compensatedWorld.getWrappedBlockStateAt(pos).getMaterial(), Materials.LAVA);
    }

    // I believe this is correct, although I'm using a method here just in case it's a tick off... I don't trust Mojang
    public boolean isSecondaryUse() {
        return player.isSneaking;
    }

    public boolean isInWater() {
        Vector3i pos = getPlacedBlockPos();
        return player.compensatedWorld.isWaterSourceBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isInLiquid() {
        Vector3i pos = getPlacedBlockPos();
        BaseBlockState data = player.compensatedWorld.getWrappedBlockStateAt(pos);
        return Materials.isWater(player.getClientVersion(), data) || Materials.checkFlag(data.getMaterial(), Materials.LAVA);
    }

    public Material getBelowMaterial() {
        return getBelowState().getMaterial();
    }

    public boolean isOn(Material... mat) {
        Material lookingFor = getBelowMaterial();
        return Arrays.stream(mat).anyMatch(m -> m == lookingFor);
    }

    public boolean isOnDirt() {
        return isOn(Material.DIRT, Material.GRASS_BLOCK, Material.PODZOL, Material.COARSE_DIRT, Material.MYCELIUM, Material.ROOTED_DIRT, Material.MOSS_BLOCK);
    }

    // I have to be the first anticheat to actually account for this... wish me luck
    // It's interested that redstone code is actually really simple, but has so many quirks
    // we don't need to account for these quirks though as they are more related to block updates.
    public boolean isBlockPlacedPowered() {
        Vector3i placed = getPlacedBlockPos();

        for (BlockFace face : BlockFace.CARTESIAN_VALUES) {
            Vector3i modified = placed.clone();

            modified.setX(placed.getX() + face.getModX());
            modified.setY(placed.getY() + face.getModY());
            modified.setZ(placed.getZ() + face.getModZ());

            // A block next to the player is providing power.  Therefore the block is powered
            if (player.compensatedWorld.getRawPowerAtState(face, modified.getX(), modified.getY(), modified.getZ()) > 0) {
                return true;
            }

            // Check if a block can even provide power... bukkit doesn't have a method for this?
            BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(modified);

            boolean isByDefaultConductive = !Materials.isSolidBlockingBlacklist(state.getMaterial(), player.getClientVersion()) &&
                    CollisionData.getData(state.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), state).isFullBlock();

            // Soul sand is exempt from this check.
            // Glass, moving pistons, beacons, redstone blocks (for some reason) and observers are not conductive
            // Otherwise, if something is solid blocking and a full block, then it is conductive
            if (state.getMaterial() != SOUL_SAND &&
                    Materials.checkFlag(state.getMaterial(), Materials.GLASS_BLOCK) || state.getMaterial() == Material.MOVING_PISTON
                    || state.getMaterial() == Material.BEACON || state.getMaterial() ==
                    Material.REDSTONE_BLOCK || state.getMaterial() == Material.OBSERVER || !isByDefaultConductive) {
                continue;
            }

            // There's a better way to do this, but this is "good enough"
            // Mojang probably does it in a worse way than this.
            for (BlockFace recursive : BlockFace.CARTESIAN_VALUES) {
                Vector3i poweredRecursive = placed.clone();

                poweredRecursive.setX(modified.getX() + recursive.getModX());
                poweredRecursive.setY(modified.getY() + recursive.getModY());
                poweredRecursive.setZ(modified.getZ() + recursive.getModZ());

                // A block next to the player is directly powered.  Therefore, the block is powered
                if (player.compensatedWorld.getDirectSignalAtState(recursive, poweredRecursive.getX(), poweredRecursive.getY(), poweredRecursive.getZ()) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public BlockFace[] getHorizontalFaces() {
        return BY_2D;
    }

    public BlockFace getDirection() {
        return face;
    }

    private List<BlockFace> getNearestLookingDirections() {
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

    // Copied from vanilla nms
    public List<BlockFace> getNearestPlacingDirections() {
        BlockFace[] faces = getNearestLookingDirections().toArray(new BlockFace[0]);

        if (!isReplaceClicked()) {
            BlockFace direction = getDirection();

            // Blame mojang for this code, not me
            int i;
            for (i = 0; i < faces.length && faces[i] != direction.getOppositeFace(); ++i) {
            }

            if (i > 0) {
                System.arraycopy(faces, 0, faces, 1, i);
                faces[0] = direction.getOppositeFace();
            }
        }

        return Arrays.asList(faces);
    }

    public boolean isFaceVertical() {
        return !isFaceHorizontal();
    }

    public boolean isFaceHorizontal() {
        BlockFace face = getDirection();
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    public boolean isXAxis() {
        BlockFace face = getDirection();
        return face == BlockFace.WEST || face == BlockFace.EAST;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public Vector3i getPlacedBlockPos() {
        if (replaceClicked) return blockPosition.clone();

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
        blockPos.setX(blockPos.getX() + face.getModX());
        blockPos.setY(blockPos.getY() + face.getModY());
        blockPos.setZ(blockPos.getZ() + face.getModZ());
        set(blockPos, state);
    }

    public void set(Vector3i position, BaseBlockState state) {
        CollisionBox box = CollisionData.getData(state.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), state, position.getX(), position.getY(), position.getZ());

        // A player cannot place a block in themselves.  THANKS MOJANG THIS CAN DESYNC BECAUSE OF ZERO POINT ZERO FUCKING THREE!
        // Great job!  It's only been an issue for years!  One fucking second to fix but you are too incompetent to change a single value.
        if (box.isIntersected(player.boundingBox)) {
            return;
        }

        // If a block already exists here, then we can't override it.
        BaseBlockState existingState = player.compensatedWorld.getWrappedBlockStateAt(position);
        WrappedBlockDataValue wrappedExisting = WrappedBlockData.getMaterialData(existingState).getData(existingState);
        if (!canBeReplaced(material, existingState, wrappedExisting)) {
            return;
        }

        if (state instanceof FlatBlockState) {
            Bukkit.broadcastMessage("Placed " + ((FlatBlockState) state).getBlockData().getAsString(false) + " at " + position);
        }

        player.compensatedWorld.updateBlock(position.getX(), position.getY(), position.getZ(), state.getCombinedId());
    }

    public boolean isZAxis() {
        BlockFace face = getDirection();
        return face == BlockFace.NORTH || face == BlockFace.SOUTH;
    }

    // We need to now run block
    public void tryCascadeBlockUpdates(Vector3i pos) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) return;

        cascadeBlockUpdates(pos);
    }

    private void cascadeBlockUpdates(Vector3i pos) {

    }

    public void set(BlockData state) {
        set(new FlatBlockState(state));
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
        Vector look = ReachUtils.getLook(player, player.xRot, player.yRot);

        Vector eyePos = new Vector(player.x, player.y + player.getEyeHeight(), player.z);
        Vector endReachPos = eyePos.clone().add(new Vector(look.getX() * 6, look.getY() * 6, look.getZ() * 6));
        Vector intercept = ReachUtils.calculateIntercept(box, eyePos, endReachPos).getFirst();

        // Bring this back to relative to the block
        // The player didn't even click the block... (we should force resync BEFORE we get here!)
        if (intercept == null) return new Vector();

        intercept.setX(intercept.getX() - box.minX);
        intercept.setY(intercept.getY() - box.minY);
        intercept.setZ(intercept.getZ() - box.minZ);

        return intercept;
    }

    // This is wrong, we need next tick's look vector because mojang is shit at netcode...
    // FOR FUCKS SAKE MOJANG WHY DIDN'T YOU FIX THIS WHEN YOU "FIXED" THE BUCKET DESYNC!
    // Are you that incompetent???  Fix the root cause!
    public BlockFace getPlayerFacing() {
        return BY_2D[GrimMath.floor(player.xRot / 90.0D + 0.5D) & 3];
    }

    public void set() {
        set(getMaterial());
    }

    public void setAbove() {
        Vector3i placed = getPlacedBlockPos();
        placed.setY(placed.getY() + 1);
        set(placed, BlockStateHelper.create(material));
    }

    public void setAbove(BaseBlockState toReplaceWith) {
        Vector3i placed = getPlacedBlockPos();
        placed.setY(placed.getY() + 1);
        set(placed, toReplaceWith);
    }
}
