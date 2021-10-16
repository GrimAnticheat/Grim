package ac.grim.grimac.utils.blockplace;

import ac.grim.grimac.utils.anticheat.Version;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedSlab;
import ac.grim.grimac.utils.blockdata.types.WrappedSnow;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.BlockStateHelper;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
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
            data.setFacing(BlockPlace.getClockWise(place.getPlayerFacing()));
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
