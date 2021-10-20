package ac.grim.grimac.utils.blockplace;

import ac.grim.grimac.utils.anticheat.Version;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedSlab;
import ac.grim.grimac.utils.blockdata.types.WrappedSnow;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.helper.BlockFaceHelper;
import ac.grim.grimac.utils.blockstate.helper.BlockStateHelper;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.*;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum BlockPlaceResult {

    // If the block only has directional data
    // TODO: Add skulls to this
    ANVIL((player, place) -> {
        if (Version.isFlat()) {
            Directional data = (Directional) place.getMaterial().createBlockData();
            data.setFacing(BlockFaceHelper.getClockWise(place.getPlayerFacing()));
            place.set(new FlatBlockState(data));
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),

    // The client only predicts one of the individual bed blocks, interestingly
    BED((player, place) -> {
        BlockFace facing = place.getPlayerFacing();
        if (place.isBlockFaceOpen(facing)) {
            place.set(place.getMaterial());
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK")).toArray(Material[]::new)),

    SNOW((player, place) -> {
        Vector3i against = place.getPlacedAgainstBlockLocation();
        WrappedBlockDataValue blockState = place.getPlacedAgainstData();
        if (blockState instanceof WrappedSnow) {
            int layers = ((WrappedSnow) blockState).getLayers() + 1; // wtf bukkit don't index at 1
            Snow snow = (Snow) Material.SNOW.createBlockData();
            snow.setLayers(layers + 1);
            place.set(against, new FlatBlockState(snow));
        } else {
            Snow snow = (Snow) Material.SNOW.createBlockData();
            snow.setLayers(1);
            place.set(against, new FlatBlockState(snow));
        }
    }, XMaterial.SNOW.parseMaterial()),

    SLAB((player, place) -> {
        Vector clickedPos = place.getClickedLocation();
        Slab slabData = (Slab) place.getMaterial().createBlockData();
        WrappedBlockDataValue existing = place.getPlacedAgainstData();

        boolean clickedTop = clickedPos.getY() > 0.5;

        if (existing instanceof WrappedSlab && place.isFaceVertical()) {
            slabData.setType(Slab.Type.DOUBLE);
            place.set(place.getPlacedAgainstBlockLocation(), new FlatBlockState(slabData));
        } else {
            slabData.setType(clickedTop ? Slab.Type.TOP : Slab.Type.BOTTOM);
            place.set(new FlatBlockState(slabData));
        }

    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("_SLAB") || mat.name().contains("STEP"))
            && !mat.name().contains("DOUBLE")).toArray(Material[]::new)),

    END_ROD((player, place) -> {
        Directional endRod = (Directional) place.getMaterial().createBlockData();
        endRod.setFacing(place.getBlockFace());
        place.set(endRod);
    }, XMaterial.END_ROD.parseMaterial(), XMaterial.LIGHTNING_ROD.parseMaterial()),

    LADDER((player, place) -> {
        // Horizontal ladders aren't a thing
        if (place.isFaceVertical()) return;

        Directional ladder = (Directional) place.getMaterial().createBlockData();
        ladder.setFacing(place.getBlockFace());
        place.set(ladder);
    }, XMaterial.LADDER.parseMaterial()),

    FARM_BLOCK((player, place) -> {
        // I need brightness to know whether this block place was successful
        // I also need heightmaps
        // Probably just mark this as a desync'd block and ignore medium sized offsets until it is resync'd
        place.set(place.getMaterial());
    }, XMaterial.FARMLAND.parseMaterial()),

    // 1.13+ only blocks from here below!  No need to write everything twice
    AMETHYST_CLUSTER((player, place) -> {
        AmethystCluster amethyst = (AmethystCluster) place.getMaterial().createBlockData();
        amethyst.setFacing(place.getBlockFace());
        place.set(amethyst);
    }, XMaterial.AMETHYST_CLUSTER.parseMaterial()),

    BAMBOO((player, place) -> {
        Vector3i clicked = place.getPlacedAgainstBlockLocation();
        if (player.compensatedWorld.getFluidLevelAt(clicked.getX(), clicked.getY(), clicked.getZ()) > 0) return;

        BaseBlockState below = place.getBelowState();
        if (Tag.BAMBOO_PLANTABLE_ON.isTagged(below.getMaterial())) {
            if (below.getMaterial() == Material.BAMBOO_SAPLING || below.getMaterial() == Material.BAMBOO) {
                place.set(Material.BAMBOO);
            } else {
                BaseBlockState above = place.getBelowState();
                if (above.getMaterial() == Material.BAMBOO_SAPLING || above.getMaterial() == Material.BAMBOO) {
                    place.set(Material.BAMBOO);
                } else {
                    place.set(Material.BAMBOO_SAPLING);
                }
            }
        }
    }, XMaterial.BAMBOO.parseMaterial(), XMaterial.BAMBOO_SAPLING.parseMaterial()),

    BELL((player, place) -> {
        Direction direction = place.getDirection();
        Bell bell = (Bell) place.getMaterial().createBlockData();

        boolean canSurvive = !Materials.checkFlag(place.getPlacedAgainstMaterial(), Materials.GATE);
        // This is exempt from being able to place on
        if (!canSurvive) return;

        if (place.isFaceVertical()) {
            if (direction == Direction.DOWN) {
                bell.setAttachment(Bell.Attachment.CEILING);
                canSurvive = place.isFaceFullCenter(BlockFace.UP);
            }
            if (direction == Direction.UP) {
                bell.setAttachment(Bell.Attachment.FLOOR);
                canSurvive = place.isFullFace(BlockFace.DOWN);
            }
            bell.setFacing(place.getPlayerFacing());
        } else {
            boolean flag = place.isXAxis()
                    && place.isFullFace(BlockFace.EAST)
                    && place.isFullFace(BlockFace.WEST)

                    || place.isZAxis()
                    && place.isFullFace(BlockFace.SOUTH)
                    && place.isFullFace(BlockFace.NORTH);

            bell.setFacing(place.getBlockFace().getOppositeFace());
            bell.setAttachment(flag ? Bell.Attachment.DOUBLE_WALL : Bell.Attachment.SINGLE_WALL);
            canSurvive = place.isFullFace(place.getBlockFace().getOppositeFace());

            if (canSurvive) {
                place.set(bell);
                return;
            }

            boolean flag1 = place.isFullFace(BlockFace.DOWN);
            bell.setAttachment(flag1 ? Bell.Attachment.FLOOR : Bell.Attachment.CEILING);
            canSurvive = place.isFullFace(flag1 ? BlockFace.DOWN : BlockFace.UP);
        }
        if (canSurvive) place.set(bell);
    }, XMaterial.BELL.parseMaterial()),

    CANDLE((player, place) -> {
        BlockData existing = place.getExistingBlockBlockData();
        Candle candle = (Candle) place.getMaterial().createBlockData();

        if (existing instanceof Candle) {
            Candle existingCandle = (Candle) existing;
            // Max candles already exists
            if (existingCandle.getMaximumCandles() == existingCandle.getCandles()) return;
            candle.setCandles(existingCandle.getCandles() + 1);
        }

        place.set(candle);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE")).toArray(Material[]::new)),

    SEA_PICKLE((player, place) -> {
        BlockData existing = place.getExistingBlockBlockData();
        SeaPickle pickle = (SeaPickle) place.getMaterial().createBlockData();

        if (existing instanceof SeaPickle) {
            SeaPickle existingPickle = (SeaPickle) existing;
            // Max pickels already exist
            if (existingPickle.getMaximumPickles() == existingPickle.getPickles()) return;
            pickle.setPickles(existingPickle.getPickles() + 1);
        }

        place.set(pickle);
    }, XMaterial.SEA_PICKLE.parseMaterial()),

    CHAIN((player, place) -> {
        Chain chain = (Chain) place.getMaterial().createBlockData();
        BlockFace face = place.getBlockFace();

        switch (face) {
            case EAST:
            case WEST:
                chain.setAxis(Axis.X);
                break;
            case NORTH:
            case SOUTH:
                chain.setAxis(Axis.Z);
                break;
            case UP:
            case DOWN:
                chain.setAxis(Axis.Y);
                break;
        }

        place.set(chain);
    }, XMaterial.CHAIN.parseMaterial()),

    COCOA((player, place) -> {
        for (BlockFace face : place.getNearestLookingDirections()) {
            if (BlockFaceHelper.isFaceVertical(face)) continue;
            Material mat = place.getDirectionalState(face).getMaterial();
            if (mat == Material.JUNGLE_LOG || mat == Material.STRIPPED_JUNGLE_LOG) {
                Cocoa data = (Cocoa) place.getMaterial().createBlockData();
                data.setFacing(face);
                place.set(face, new FlatBlockState(data));
                break;
            }
        }
    }, XMaterial.COCOA.parseMaterial()),

    DIRT_PATH((player, place) -> {
        BaseBlockState state = place.getDirectionalState(BlockFace.UP);
        // If there is a solid block above the dirt path, it turns to air
        if (!Materials.checkFlag(state.getMaterial(), Materials.SOLID_BLACKLIST)) {
            place.set(place.getMaterial());
        } else {
            place.set(Material.DIRT);
        }
    }, XMaterial.DIRT_PATH.parseMaterial()),

    HOPPER((player, place) -> {
        BlockFace opposite = place.getPlayerFacing().getOppositeFace();
        Hopper hopper = (Hopper) place.getMaterial().createBlockData();
        hopper.setFacing(place.isFaceVertical() ? BlockFace.DOWN : opposite);
    }, XMaterial.HOPPER.parseMaterial()),

    LANTERN((player, place) -> {
        for (BlockFace face : place.getNearestLookingDirections()) {
            if (BlockFaceHelper.isFaceHorizontal(face)) continue;
            Lantern lantern = (Lantern) place.getMaterial().createBlockData();

            boolean isHanging = face == BlockFace.UP;
            lantern.setHanging(isHanging);

            boolean canSurvive = place.isFaceFullCenter(isHanging ? BlockFace.UP : BlockFace.DOWN) && !Materials.checkFlag(place.getPlacedAgainstMaterial(), Materials.GATE);
            if (!canSurvive) continue;

            place.set(new FlatBlockState(lantern));
            return;
        }
    }, XMaterial.LANTERN.parseMaterial(), XMaterial.SOUL_LANTERN.parseMaterial()),

    POINTED_DRIPSTONE((player, place) -> {
        // To explain what Mojang is doing, take the example of placing on top face
        BlockFace primaryDirection = place.getNearestVerticalDirection().getOppositeFace(); // The player clicked downwards, so use upwards
        BlockData typePlacingOn = place.getDirectionalFlatState(primaryDirection.getOppositeFace()).getBlockData(); // Block we are placing on

        // Check to see if we can place on the block or there is dripstone on the block that we are placing on also pointing upwards
        boolean primarySameType = typePlacingOn instanceof PointedDripstone && ((PointedDripstone) typePlacingOn).getVerticalDirection() == primaryDirection;
        boolean primaryValid = place.isFullFace(primaryDirection.getOppositeFace()) || primarySameType;

        // Try to use the opposite direction, just to see if switching directions makes it valid.
        if (!primaryValid) {
            BlockFace secondaryDirection = primaryDirection.getOppositeFace(); // See if placing it DOWNWARDS is valid
            BlockData secondaryType = place.getDirectionalFlatState(secondaryDirection.getOppositeFace()).getBlockData(); // Get the block above us
            // Check if the dripstone above us is also facing downwards
            boolean secondarySameType = secondaryType instanceof PointedDripstone && ((PointedDripstone) secondaryType).getVerticalDirection() == secondaryDirection;

            primaryDirection = secondaryDirection;
            typePlacingOn = secondaryType;
            // Update block survivability
            primaryValid = place.isFullFace(secondaryDirection.getOppositeFace()) || secondarySameType;
        }

        // No valid locations
        if (!primaryValid) return;

        PointedDripstone toPlace = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
        toPlace.setVerticalDirection(primaryDirection); // This block is facing UPWARDS as placed on the top face

        // We then have to calculate the thickness of the dripstone
        //
        // PrimaryDirection should be the direction that the current dripstone being placed will face
        // oppositeType should be the opposite to the direction the dripstone is facing, what it is pointing into
        //
        // If the dripstone is -> <- pointed at one another

        // If check the blockstate that is above now with the direction of DOWN
        BlockData oppositeToUs = place.getDirectionalFlatState(primaryDirection).getBlockData();

        // TODO: This is block update code and we must now run this for all 6 directions around us.
        if (oppositeToUs instanceof PointedDripstone && ((PointedDripstone) oppositeToUs).getVerticalDirection() == primaryDirection.getOppositeFace()) {
            PointedDripstone dripstone = (PointedDripstone) oppositeToUs;
            // Use tip if the player is sneaking, or if it already is merged (somehow)
            PointedDripstone.Thickness thick = place.isSecondaryUse() && dripstone.getThickness() != PointedDripstone.Thickness.TIP_MERGE ?
                    PointedDripstone.Thickness.TIP : PointedDripstone.Thickness.TIP_MERGE;

            toPlace.setThickness(thick);
        } else {
            BlockData sameDirectionToUs = place.getDirectionalFlatState(primaryDirection).getBlockData();

            // Check if the blockstate air does not have the direction of UP already (somehow)
            if (!(sameDirectionToUs instanceof PointedDripstone) || ((PointedDripstone) sameDirectionToUs).getVerticalDirection() != primaryDirection) {
                toPlace.setThickness(PointedDripstone.Thickness.TIP);
            } else {
                if (typePlacingOn instanceof PointedDripstone &&
                        ((PointedDripstone) typePlacingOn).getThickness() != PointedDripstone.Thickness.TIP &&
                        ((PointedDripstone) typePlacingOn).getThickness() != PointedDripstone.Thickness.TIP_MERGE) {
                    // Look downwards
                    PointedDripstone dripstone = (PointedDripstone) typePlacingOn;
                    PointedDripstone.Thickness toSetThick = dripstone.getVerticalDirection() == primaryDirection ? PointedDripstone.Thickness.BASE : PointedDripstone.Thickness.MIDDLE;
                    toPlace.setThickness(toSetThick);

                } else {
                    toPlace.setThickness(PointedDripstone.Thickness.FRUSTUM);
                }
            }
        }

        place.set(toPlace);
    }, XMaterial.POINTED_DRIPSTONE.parseMaterial()),

    PISTON_BASE((player, place) -> {
        Piston piston = (Piston) place.getMaterial().createBlockData();
        piston.setFacing(place.getNearestVerticalDirection().getOppositeFace());
    }),

    // Blocks that have both wall and standing states
    // Torches, banners, and player heads
    TORCH((player, place) -> {
        for (BlockFace face : place.getNearestLookingDirections()) {
            if (place.isFullFace(face) && face != BlockFace.UP) {
                if (BlockFaceHelper.isFaceHorizontal(face)) { // type doesn't matter to grim, same hitbox.
                    Directional dir = (Directional) Material.WALL_TORCH.createBlockData();
                    dir.setFacing(face.getOppositeFace());
                    place.set(dir);
                } else {
                    place.set(place.getMaterial());
                }
                break;
            }
        }
    }, XMaterial.TORCH.parseMaterial(), XMaterial.REDSTONE_TORCH.parseMaterial(), XMaterial.SOUL_TORCH.parseMaterial()),

    NO_DATA((player, place) -> {
        place.set(BlockStateHelper.create(place.getMaterial()));
    }, XMaterial.AIR.parseMaterial());

    private static final BlockPlaceResult[] lookup = new BlockPlaceResult[Material.values().length];

    static {
        for (BlockPlaceResult data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final BlockPlaceFactory data;
    private final Material[] materials;

    BlockPlaceResult(BlockPlaceFactory data, Material... materials) {
        this.data = data;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public static BlockPlaceFactory getMaterialData(Material placed) {
        BlockPlaceResult data = lookup[placed.ordinal()];

        return data == null ? NO_DATA.data : data.data;
    }
}
