package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.collisions.AxisSelect;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class BlockPlace {
    private static final BlockFace[] BY_3D = { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST };
    public static final BlockFace[] BY_2D = { BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST };
    public final boolean isBlock;
    // Allocated once instead of in functions to reduce new[] calls that need to be made. Since per-instance BlockPlace is always dealt with on the same thread we can use 1 buffer array
    private final SimpleCollisionBox[] collisions = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
    public Vector3i position;
    public final InteractionHand hand;
    public boolean replaceClicked;
    @Getter private boolean isCancelled;
    private final GrimPlayer player;
    public final ItemStack itemStack;
    public final StateType material;
    public final @Nullable HitData hitData;
    @Getter private int faceId;
    @Getter private BlockFace face;
    public boolean isInside;
    public Vector3f cursor;
    public final int sequence;

    public BlockPlace(GrimPlayer player, InteractionHand hand, Vector3i position, int faceId, BlockFace face, ItemStack itemStack, @Nullable HitData hitData, int sequence) {
        this.player = player;
        this.hand = hand;
        this.position = position;
        this.faceId = faceId;
        this.face = face;
        this.itemStack = itemStack;
        if (itemStack.getType().getPlacedType() == null) {
            this.material = StateTypes.FIRE;
            this.isBlock = false;
        } else {
            this.material = itemStack.getType().getPlacedType();
            this.isBlock = true;
        }
        this.hitData = hitData;

        WrappedBlockState state = player.compensatedWorld.getBlock(position);
        this.replaceClicked = canBeReplaced(material, state, face);
        this.sequence = sequence;
    }

    public WrappedBlockState getExistingBlockData() {
        return player.compensatedWorld.getBlock(getPlacedBlockPos());
    }

    public StateType getPlacedAgainstMaterial() {
        return player.compensatedWorld.getBlock(position).getType();
    }

    public WrappedBlockState getBelowState() {
        Vector3i pos = getPlacedBlockPos();
        pos = pos.withY(pos.getY() - 1);
        return player.compensatedWorld.getBlock(pos);
    }

    public WrappedBlockState getAboveState() {
        Vector3i pos = getPlacedBlockPos();
        pos = pos.withY(pos.getY() + 1);
        return player.compensatedWorld.getBlock(pos);
    }

    public WrappedBlockState getDirectionalState(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos = pos.add(facing.getModX(), facing.getModY(), facing.getModZ());
        return player.compensatedWorld.getBlock(pos);
    }

    public boolean isSolidBlocking(BlockFace relative) {
        WrappedBlockState state = getDirectionalState(relative);
        return state.getType().isBlocking();
    }

    private boolean canBeReplaced(StateType heldItem, WrappedBlockState state, BlockFace face) {
        StateType currentType = state.getType();

        // Checks are ordered by approximate frequency (Slabs/Snow/Vines first)
        // to minimize comparisons for common blocks.

        if (BlockTags.SLABS.contains(currentType)) {
            Type typeData = state.getTypeData();
            if (typeData == Type.DOUBLE || currentType != heldItem) return false;

            // Here vanilla refers from
            // Set check can replace -> get block -> call block canBeReplaced -> check can replace boolean (default true)
            // uh... what?  I'm unsure what Mojang is doing here.  I think they just made a stupid mistake.
            // as this code is quite old.
            boolean isHighClick = getClickedLocation().getY() > 0.5D;

            if (typeData == Type.BOTTOM) {
                return getFace() == BlockFace.UP || (isHighClick && isFaceHorizontal());
            } else {
                return getFace() == BlockFace.DOWN || (!isHighClick && isFaceHorizontal());
            }
        }
        else if (currentType == StateTypes.SNOW) {
            int layers = state.getLayers();
            if (heldItem == currentType && layers < 8) { // We index at 1 (less than 8 layers)
                return face == BlockFace.UP;
            }
            return layers == 1; // index at 1, (1 layer)
        }
        else if (currentType == StateTypes.VINE) {
            boolean baseReplaceable = currentType != heldItem && currentType.isReplaceable();
            if (baseReplaceable) return true;
            if (heldItem != currentType) return false;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) && !state.isUp()) {
                return true;
            }

            return state.getNorth() == North.FALSE ||
                    state.getSouth() == South.FALSE ||
                    state.getEast() == East.FALSE ||
                    state.getWest() == West.FALSE;
        }
        else if (currentType == StateTypes.LADDER) {
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
                return true;
            }
            return currentType != heldItem && currentType.isReplaceable();
        }
        // Glow lichen can be replaced if it has an open face, or the player is placing something
        // Sculk Vein shares the exact same placement logic
        else if (currentType == StateTypes.GLOW_LICHEN || currentType == StateTypes.SCULK_VEIN) {
            return heldItem != currentType ||
                    !state.isUp() ||
                    !state.isDown() ||
                    state.getNorth() == North.FALSE ||
                    state.getSouth() == South.FALSE ||
                    state.getEast() == East.FALSE ||
                    state.getWest() == West.FALSE;
        }
        else if (currentType == StateTypes.SCAFFOLDING) {
            return heldItem == StateTypes.SCAFFOLDING;
        }
        else if (BlockTags.CANDLES.contains(currentType)) {
            return heldItem == currentType && state.getCandles() < 4 && !isSecondaryUse();
        }
        else if (currentType == StateTypes.SEA_PICKLE) {
            return heldItem == currentType && state.getPickles() < 4 && !isSecondaryUse();
        }
        else if (currentType == StateTypes.TURTLE_EGG) {
            return heldItem == currentType && state.getEggs() < 4 && !isSecondaryUse();
        }
        // Cave vines and weeping vines have a special case... that always returns false (just like the base case for it!)
        else {
            return currentType != heldItem && currentType.isReplaceable();
        }
    }

    public boolean isFaceFullCenter(BlockFace facing) {
        WrappedBlockState data = getDirectionalState(facing);
        CollisionBox box = CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data);

        if (box.isNull()) return false;
        if (isFullFace(facing)) return true;
        if (BlockTags.LEAVES.contains(data.getType())) return false;
        if (BlockTags.FENCE_GATES.contains(data.getType())) return false;

        int size = box.downCast(collisions);

        AxisSelect axis = AxisSelect.byFace(facing.getOppositeFace());

        for (int i = 0; i < size; i++) {
            SimpleCollisionBox simpleBox = collisions[i];
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
        WrappedBlockState data = getDirectionalState(facing);
        CollisionBox box = CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data);

        if (box.isNull()) return false;
        if (isFullFace(facing)) return true;
        if (BlockTags.LEAVES.contains(data.getType())) return false;

        int size = box.downCast(collisions);

        AxisSelect axis = AxisSelect.byFace(facing.getOppositeFace());

        for (int i = 0; i < size; i++) {
            SimpleCollisionBox simpleBox = collisions[i];
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
        WrappedBlockState state = getDirectionalState(relative);
        BlockFace face = relative.getOppositeFace();
        BlockFace bukkitFace = BlockFace.valueOf(face.name());

        AxisSelect axis = AxisSelect.byFace(face);

        CollisionBox box = CollisionData.getData(state.getType()).getMovementCollisionBox(player, player.getClientVersion(), state);

        StateType blockMaterial = state.getType();

        if (BlockTags.LEAVES.contains(blockMaterial)) {
            // Leaves can't support blocks
            return false;
        } else if (blockMaterial == StateTypes.SNOW) {
            return state.getLayers() == 8 || face == BlockFace.DOWN;
        } else if (BlockTags.STAIRS.contains(blockMaterial)) {
            if (face == BlockFace.UP) {
                return state.getHalf() == Half.TOP;
            }
            if (face == BlockFace.DOWN) {
                return state.getHalf() == Half.BOTTOM;
            }

            return state.getFacing() == bukkitFace;
        } else if (blockMaterial == StateTypes.COMPOSTER) { // Composters have solid faces except for on the top
            return face != BlockFace.UP;
        } else if (blockMaterial == StateTypes.SOUL_SAND) { // Soul sand is considered to be a full block when placing things
            return true;
        } else if (blockMaterial == StateTypes.LADDER) { // Yes, although it breaks immediately, you can place blocks on ladders
            return state.getFacing().getOppositeFace() == bukkitFace;
        } else if (BlockTags.TRAPDOORS.contains(blockMaterial)) { // You can place blocks that need solid faces on trapdoors
            return (state.getFacing().getOppositeFace() == bukkitFace && state.isOpen()) ||
                    (state.getHalf() == Half.TOP && !state.isOpen() && bukkitFace == BlockFace.UP) ||
                    (state.getHalf() == Half.BOTTOM && !state.isOpen() && bukkitFace == BlockFace.DOWN);
        } else if (BlockTags.DOORS.contains(blockMaterial)) { // You can place blocks that need solid faces on doors
            CollisionData data = CollisionData.getData(blockMaterial);

            if (data.dynamic instanceof DoorHandler doorHandler) {
                return doorHandler.fetchDirection(
                        player, player.getClientVersion(), state,
                        position.x, position.y, position.z
                ).getOppositeFace() == bukkitFace;
            }
        }

        int size = box.downCast(collisions);

        for (int i = 0; i < size; i++) {
            SimpleCollisionBox simpleBox = collisions[i];
            if (axis.modify(simpleBox).isFullBlockNoCache()) return true;
        }

        // Not an explicit edge case and is complicated, so isn't a full face
        return false;
    }

    public boolean isBlockFaceOpen(BlockFace facing) {
        Vector3i pos = getPlacedBlockPos();
        pos = pos.add(facing.getModX(), facing.getModY(), facing.getModZ());
        // You can't build above height limit.
        if (pos.getY() >= player.compensatedWorld.getMaxHeight()) return false;

        return player.compensatedWorld.getBlock(pos).getType().isReplaceable();
    }

    public boolean isFaceEmpty(BlockFace facing) {
        WrappedBlockState data = getDirectionalState(facing);
        CollisionBox box = CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data);

        if (box.isNull()) return false;
        if (isFullFace(facing)) return true;
        if (BlockTags.LEAVES.contains(data.getType())) return false;

        int size = box.downCast(collisions);

        AxisSelect axis = AxisSelect.byFace(facing.getOppositeFace());

        for (int i = 0; i < size; i++) {
            SimpleCollisionBox simpleBox = collisions[i];
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
        pos = pos.add(facing.getModX(), facing.getModY(), facing.getModZ());
        return player.compensatedWorld.getBlock(pos).getType() == StateTypes.LAVA;
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
        WrappedBlockState data = player.compensatedWorld.getBlock(pos);
        return Materials.isWater(player.getClientVersion(), data) || data.getType() == StateTypes.LAVA;
    }

    public StateType getBelowMaterial() {
        return getBelowState().getType();
    }

    public boolean isOn(StateType... mat) {
        StateType lookingFor = getBelowMaterial();
        return Arrays.stream(mat).anyMatch(m -> m == lookingFor);
    }

    public boolean isOnDirt() {
        return isOn(StateTypes.DIRT, StateTypes.GRASS_BLOCK, StateTypes.PODZOL, StateTypes.COARSE_DIRT, StateTypes.MYCELIUM, StateTypes.ROOTED_DIRT, StateTypes.MOSS_BLOCK);
    }

    // I have to be the first anticheat to actually account for this... wish me luck
    // It's interested that redstone code is actually really simple, but has so many quirks
    // we don't need to account for these quirks though as they are more related to block updates.
    public boolean isBlockPlacedPowered() {
        Vector3i placed = getPlacedBlockPos();

        for (BlockFace face : BY_3D) {
            Vector3i modified = placed.add(face.getModX(), face.getModY(), face.getModZ());

            // A block next to the player is providing power. Therefore the block is powered
            if (player.compensatedWorld.getRawPowerAtState(face, modified.getX(), modified.getY(), modified.getZ()) > 0) {
                return true;
            }

            // Check if a block can even provide power... bukkit doesn't have a method for this?
            WrappedBlockState state = player.compensatedWorld.getBlock(modified);

            boolean isByDefaultConductive = !Materials.isSolidBlockingBlacklist(state.getType(), player.getClientVersion()) &&
                    CollisionData.getData(state.getType()).getMovementCollisionBox(player, player.getClientVersion(), state).isFullBlock();

            // Soul sand is exempt from this check.
            // Glass, moving pistons, beacons, redstone blocks (for some reason) and observers are not conductive
            // Otherwise, if something is solid blocking and a full block, then it is conductive
            if (state.getType() != StateTypes.SOUL_SAND &&
                    BlockTags.GLASS_BLOCKS.contains(state.getType()) || state.getType() == StateTypes.MOVING_PISTON
                    || state.getType() == StateTypes.BEACON || state.getType() ==
                    StateTypes.REDSTONE_BLOCK || state.getType() == StateTypes.OBSERVER || !isByDefaultConductive) {
                continue;
            }

            // There's a better way to do this, but this is "good enough"
            // Mojang probably does it in a worse way than this.
            for (BlockFace recursive : BY_3D) {
                Vector3i poweredRecursive = placed.add(recursive.getModX(), recursive.getModY(), recursive.getModZ());

                // A block next to the player is directly powered.  Therefore, the block is powered
                if (player.compensatedWorld.getDirectSignalAtState(recursive, poweredRecursive.getX(), poweredRecursive.getY(), poweredRecursive.getZ()) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public void setFace(BlockFace face) {
        this.face = face;
        this.faceId = face.getFaceValue();
    }

    public void setFaceId(int face) {
        this.faceId = face;
        this.face = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) ? BlockFace.getBlockFaceByValue(faceId) : BlockFace.getLegacyBlockFaceByValue(faceId);
    }

    private List<BlockFace> getNearestLookingDirections() {
        float pitch = GrimMath.radians(player.pitch);
        float yaw = GrimMath.radians(-player.yaw);
        float y = player.trigHandler.sin(pitch);
        float cosPitch = player.trigHandler.cos(pitch);
        float x = player.trigHandler.sin(yaw);
        float z = player.trigHandler.cos(yaw);

        boolean isPositiveX = x > 0;
        boolean isNegativeY = y < 0;
        boolean isPositiveZ = z > 0;

        float absX = isPositiveX ? x : -x;
        float absY = isNegativeY ? -y : y;
        float absZ = isPositiveZ ? z : -z;
        float modifiedX = absX * cosPitch;
        float modifiedZ = absZ * cosPitch;

        BlockFace xDir = isPositiveX ? BlockFace.EAST : BlockFace.WEST;
        BlockFace yDir = isNegativeY ? BlockFace.UP : BlockFace.DOWN;
        BlockFace zDir = isPositiveZ ? BlockFace.SOUTH : BlockFace.NORTH;

        if (absX > absZ) {
            if (absY > modifiedX) {
                return makeDirList(yDir, xDir, zDir);
            } else {
                return modifiedZ > absY ? makeDirList(xDir, zDir, yDir) : makeDirList(xDir, yDir, zDir);
            }
        } else if (absY > modifiedZ) {
            return makeDirList(yDir, zDir, xDir);
        } else {
            return modifiedX > absY ? makeDirList(zDir, xDir, yDir) : makeDirList(zDir, yDir, xDir);
        }
    }

    private List<BlockFace> makeDirList(BlockFace one, BlockFace two, BlockFace three) {
        return Arrays.asList(one, two, three, three.getOppositeFace(), two.getOppositeFace(), one.getOppositeFace());
    }

    public BlockFace getNearestVerticalDirection() {
        return player.pitch < 0.0F ? BlockFace.UP : BlockFace.DOWN;
    }

    // Copied from vanilla nms
    public List<BlockFace> getNearestPlacingDirections() {
        BlockFace[] faces = getNearestLookingDirections().toArray(new BlockFace[0]);

        if (!replaceClicked) {
            BlockFace direction = getFace();

            // Blame mojang for this code, not me
            int i = 0;
            while (i < faces.length && faces[i] != direction.getOppositeFace()) i++;

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
        BlockFace face = getFace();
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    public boolean isXAxis() {
        BlockFace face = getFace();
        return face == BlockFace.WEST || face == BlockFace.EAST;
    }

    public Vector3i getPlacedBlockPos() {
        if (replaceClicked) return position;

        int x = position.getX() + getNormalBlockFace().getX();
        int y = position.getY() + getNormalBlockFace().getY();
        int z = position.getZ() + getNormalBlockFace().getZ();
        return new Vector3i(x, y, z);
    }

    public Vector3i getNormalBlockFace() {
        return switch (face) {
            case DOWN -> new Vector3i(0, -1, 0);
            case SOUTH -> new Vector3i(0, 0, 1);
            case NORTH -> new Vector3i(0, 0, -1);
            case WEST -> new Vector3i(-1, 0, 0);
            case EAST -> new Vector3i(1, 0, 0);
            default -> new Vector3i(0, 1, 0);
        };
    }

    public void set(StateType material) {
        set(material.createBlockState(CompensatedWorld.blockVersion));
    }

    public void set(BlockFace face, WrappedBlockState state) {
        Vector3i blockPos = getPlacedBlockPos().add(face.getModX(), face.getModY(), face.getModZ());
        set(blockPos, state);
    }

    public void set(Vector3i position, WrappedBlockState state) {
        // Hack for scaffolding to be the correct bounding box
        CollisionBox box = CollisionData.getData(state.getType()).getMovementCollisionBox(player, player.getClientVersion(), state, position.getX(), position.getY(), position.getZ());


        // Note scaffolding is a special case because it can never intersect with the player's bounding box,
        // and we fetch it with lastY instead of y which is wrong, so it is easier to just ignore scaffolding here
        if (state.getType() != StateTypes.SCAFFOLDING) {
            // A player cannot place a block in themselves.
            // 0.03 can desync quite easily
            // 0.002 desync must be done with teleports, it is very difficult to do with slightly moving.
            if (box.isIntersected(player.boundingBox)) {
                return;
            }

            // Other entities can also block block-placing
            // This sucks and desyncs constantly, but what can you do?
            //
            // 1.9+ introduced the mechanic where both the client and server must agree upon a block place
            // 1.8 clients will simply not send the place when it fails, thanks mojang.
            if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
                for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                    if (!entity.canHit()) continue;
                    SimpleCollisionBox interpBox = entity.getPossibleCollisionBoxes();

                    final double scale = entity.getAttributeValue(Attributes.SCALE);
                    double width = BoundingBoxSize.getWidth(player, entity) * scale;
                    double height = BoundingBoxSize.getHeight(player, entity) * scale;
                    double interpWidth = Math.max(interpBox.maxX - interpBox.minX, interpBox.maxZ - interpBox.minZ);
                    double interpHeight = interpBox.maxY - interpBox.minY;

                    // If not accurate, fall back to desync pos
                    // This happens due to the lack of an idle packet on 1.9+ clients
                    // On 1.8 clients this should practically never happen
                    if (interpWidth - width > 0.05 || interpHeight - height > 0.05) {
                        Vector3d entityPos = entity.trackedServerPosition.getPos();
                        interpBox = GetBoundingBox.getPacketEntityBoundingBox(player, entityPos.getX(), entityPos.getY(), entityPos.getZ(), entity);
                    }

                    if (box.isIntersected(interpBox)) {
                        return; // Blocking the block placement
                    }
                }
            }
        }

        // If a block already exists here, then we can't override it.
        WrappedBlockState existingState = player.compensatedWorld.getBlock(position);
        if (!replaceClicked && !canBeReplaced(material, existingState, face)) {
            return;
        }

        // Check for min and max bounds of world
        if (player.compensatedWorld.getMaxHeight() <= position.getY() || position.getY() < player.compensatedWorld.getMinHeight()) {
            return;
        }

        // Check for waterlogged
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            if (state.hasProperty(StateValue.WATERLOGGED)) { // waterloggable
                state.setWaterlogged(existingState.getType() == StateTypes.WATER && existingState.getLevel() == 0);
            }
        }

        player.inventory.onBlockPlace(this);
        player.compensatedWorld.updateBlock(position.getX(), position.getY(), position.getZ(), state.getGlobalId());
    }

    public boolean isZAxis() {
        BlockFace face = getFace();
        return face == BlockFace.NORTH || face == BlockFace.SOUTH;
    }

    // We need to now run block
    public void tryCascadeBlockUpdates(Vector3i pos) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2)) return;

        cascadeBlockUpdates(pos);
    }

    private void cascadeBlockUpdates(Vector3i pos) {

    }

    public void set(WrappedBlockState state) {
        set(getPlacedBlockPos(), state);
    }

    public void resync() {
        isCancelled = true;
    }

    // All method with rants about mojang must go below this line

    // MOJANG??? Why did you remove this from the damn packet.  YOU DON'T DO BLOCK PLACING RIGHT!
    // You use last tick vector on the server and current tick on the client...
    // You also have 0.03 for FIVE YEARS which will mess this up.  nice one mojang
    // * 0.0004 as of 2/24/2022
    // Fix your damn netcode
    //
    // You also have the desync caused by eye height as apparently tracking the player's ticks wasn't important to you
    // No mojang, you really do need to track client ticks to get their accurate eye height.
    // another damn desync added... maybe next decade it will get fixed and double the amount of issues.
    public Vector3dm getClickedLocation() {
        SimpleCollisionBox box = new SimpleCollisionBox(position);
        Vector3dm look = ReachUtils.getLook(player, player.yaw, player.pitch);

        final double distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 3;
        Vector3dm eyePos = new Vector3dm(player.x, player.y + player.getEyeHeight(), player.z);
        Vector3dm endReachPos = eyePos.clone().add(new Vector3dm(look.getX() * distance, look.getY() * distance, look.getZ() * distance));
        Vector3dm intercept = ReachUtils.calculateIntercept(box, eyePos, endReachPos).first();

        // Bring this back to relative to the block
        // The player didn't even click the block... (we should force resync BEFORE we get here!)
        if (intercept == null) return new Vector3dm();

        intercept.setX(intercept.getX() - box.minX);
        intercept.setY(intercept.getY() - box.minY);
        intercept.setZ(intercept.getZ() - box.minZ);

        return intercept;
    }

    // Remember to use the next tick's look, which we handle elsewhere
    public BlockFace getPlayerFacing() {
        return BY_2D[GrimMath.floor(player.yaw / 90.0D + 0.5D) & 3];
    }

    public void set() {
        if (material == null) {
            LogUtil.warn("Material " + null + " has no placed type!");
            return;
        }
        set(material);
    }

    public void setAbove() {
        Vector3i placed = getPlacedBlockPos();
        placed = placed.add(0, 1, 0);
        set(placed, material.createBlockState(CompensatedWorld.blockVersion));
    }

    public void setAbove(WrappedBlockState toReplaceWith) {
        Vector3i placed = getPlacedBlockPos();
        placed = placed.add(0, 1, 0);
        set(placed, toReplaceWith);
    }
}
