package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.util.HashSet;
import java.util.Set;

public class Materials {
    private static final Set<StateType> NO_PLACE_LIQUIDS = new HashSet<>();
    // Includes iron panes in addition to glass panes
    private static final Set<StateType> PANES = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS_LEGACY = new HashSet<>();
    private static final Set<StateType> LAVA_LIQUIDS = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES_LEGACY = new HashSet<>();

    private static final Set<StateType> CLIENT_SIDE = new HashSet<>();

    private static final Set<StateType> SHAPE_EXCEEDS_CUBE = new HashSet<>();

    static {
        // Lava hasn't changed, other than STATIONARY_LAVA material on 1.12- servers
        LAVA_LIQUIDS.add(StateTypes.LAVA);

        // Base water, flowing on 1.12- but not on 1.13+ servers
        WATER_LIQUIDS.add(StateTypes.WATER);
        WATER_LIQUIDS_LEGACY.add(StateTypes.WATER);

        // Becomes grass for legacy versions
        WATER_LIQUIDS.add(StateTypes.KELP);
        WATER_SOURCES.add(StateTypes.KELP);
        WATER_LIQUIDS.add(StateTypes.KELP_PLANT);
        WATER_SOURCES.add(StateTypes.KELP_PLANT);

        // Is translated to air for legacy versions
        WATER_SOURCES.add(StateTypes.BUBBLE_COLUMN);
        WATER_LIQUIDS_LEGACY.add(StateTypes.BUBBLE_COLUMN);
        WATER_LIQUIDS.add(StateTypes.BUBBLE_COLUMN);
        WATER_SOURCES_LEGACY.add(StateTypes.BUBBLE_COLUMN);

        // This is not water on 1.12- players
        WATER_SOURCES.add(StateTypes.SEAGRASS);
        WATER_LIQUIDS.add(StateTypes.SEAGRASS);

        // This is not water on 1.12- players`
        WATER_SOURCES.add(StateTypes.TALL_SEAGRASS);
        WATER_LIQUIDS.add(StateTypes.TALL_SEAGRASS);

        NO_PLACE_LIQUIDS.add(StateTypes.WATER);
        NO_PLACE_LIQUIDS.add(StateTypes.LAVA);

        // Important blocks where we need to ignore right-clicking on for placing blocks
        // We can ignore stuff like right-clicking a pumpkin with shears...
        CLIENT_SIDE.add(StateTypes.BARREL);
        CLIENT_SIDE.add(StateTypes.BEACON);
        CLIENT_SIDE.add(StateTypes.BREWING_STAND);
        CLIENT_SIDE.add(StateTypes.CARTOGRAPHY_TABLE);
        CLIENT_SIDE.add(StateTypes.CHEST);
        CLIENT_SIDE.add(StateTypes.TRAPPED_CHEST);
        CLIENT_SIDE.add(StateTypes.COMPARATOR);
        CLIENT_SIDE.add(StateTypes.CRAFTING_TABLE);
        CLIENT_SIDE.add(StateTypes.DAYLIGHT_DETECTOR);
        CLIENT_SIDE.add(StateTypes.DISPENSER);
        CLIENT_SIDE.add(StateTypes.DRAGON_EGG);
        CLIENT_SIDE.add(StateTypes.ENCHANTING_TABLE);
        CLIENT_SIDE.add(StateTypes.ENDER_CHEST);
        CLIENT_SIDE.add(StateTypes.GRINDSTONE);
        CLIENT_SIDE.add(StateTypes.HOPPER);
        CLIENT_SIDE.add(StateTypes.LEVER);
        CLIENT_SIDE.add(StateTypes.LIGHT);
        CLIENT_SIDE.add(StateTypes.LOOM);
        CLIENT_SIDE.add(StateTypes.NOTE_BLOCK);
        CLIENT_SIDE.add(StateTypes.REPEATER);
        CLIENT_SIDE.add(StateTypes.SMITHING_TABLE);
        CLIENT_SIDE.add(StateTypes.STONECUTTER);

        CLIENT_SIDE.addAll(BlockTags.FENCE_GATES.getStates());
        CLIENT_SIDE.addAll(BlockTags.ANVIL.getStates());
        CLIENT_SIDE.addAll(BlockTags.BEDS.getStates());
        CLIENT_SIDE.addAll(BlockTags.BUTTONS.getStates());
        CLIENT_SIDE.addAll(BlockTags.SHULKER_BOXES.getStates());
        CLIENT_SIDE.addAll(BlockTags.SIGNS.getStates());
        CLIENT_SIDE.addAll(BlockTags.FLOWER_POTS.getStates());
        CLIENT_SIDE.addAll(BlockTags.TRAPDOORS.getStates());

        PANES.addAll(BlockTags.GLASS_PANES.getStates());
        PANES.add(StateTypes.IRON_BARS);

        SHAPE_EXCEEDS_CUBE.addAll(BlockTags.FENCES.getStates());
        SHAPE_EXCEEDS_CUBE.addAll(BlockTags.FENCE_GATES.getStates());
        SHAPE_EXCEEDS_CUBE.addAll(BlockTags.WALLS.getStates());
    }

    public static boolean isStairs(StateType type) {
        return BlockTags.STAIRS.contains(type);
    }

    public static boolean isSlab(StateType type) {
        return BlockTags.SLABS.contains(type);
    }

    public static boolean isWall(StateType type) {
        return BlockTags.WALLS.contains(type);
    }

    public static boolean isButton(StateType type) {
        return BlockTags.BUTTONS.contains(type);
    }

    public static boolean isFence(StateType type) {
        return BlockTags.FENCES.contains(type);
    }

    public static boolean isGate(StateType type) {
        return BlockTags.FENCE_GATES.contains(type);
    }

    public static boolean isBed(StateType type) {
        return BlockTags.BEDS.contains(type);
    }

    public static boolean isAir(StateType type) {
        return type.isAir();
    }

    public static boolean isLeaves(StateType type) {
        return BlockTags.LEAVES.contains(type);
    }

    public static boolean isDoor(StateType type) {
        return BlockTags.DOORS.contains(type);
    }

    public static boolean isShulker(StateType type) {
        return BlockTags.SHULKER_BOXES.contains(type);
    }

    public static boolean isGlassBlock(StateType type) {
        return BlockTags.GLASS_BLOCKS.contains(type);
    }

    public static boolean isGlassPane(StateType type) {
        return PANES.contains(type);
    }

    public static boolean isClimbable(StateType type) {
        return BlockTags.CLIMBABLE.contains(type);
    }

    public static boolean isCauldron(StateType type) {
        return BlockTags.CAULDRONS.contains(type);
    }

    public static boolean isWaterModern(StateType type) {
        return WATER_LIQUIDS.contains(type);
    }

    public static boolean isWaterLegacy(StateType type) {
        return WATER_LIQUIDS_LEGACY.contains(type);
    }

    public static boolean isShapeExceedsCube(StateType type) {
        return SHAPE_EXCEEDS_CUBE.contains(type);
    }

    public static boolean isUsable(ItemType material) {
        return material != null && (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                || material == ItemTypes.CROSSBOW || material == ItemTypes.BOW || material.toString().endsWith("SWORD")
                || material == ItemTypes.TRIDENT || material == ItemTypes.SHIELD);
    }

    public static boolean isWater(ClientVersion clientVersion, WrappedBlockState state) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            return isWaterModern(state.getType());
        }
        return clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13) ? (WATER : WATER_LEGACY)
                || isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, WrappedBlockState state) {
        if (clientVersion.isOlderThanOrEquals(ClientVersion.V_1_12_2)) return false;
        if (!ItemTypes.isNewVersion()) return false;

        FlatBlockState flat = (FlatBlockState) state;
        BlockData blockData = flat.getBlockData();
        Material mat = blockData.getMaterial();

        // Waterlogged lanterns were added in 1.16.2
        if (clientVersion.isOlderThan(ClientVersion.V_1_16_2) && (mat == LANTERN || mat == SOUL_LANTERN))
            return false;
        // ViaVersion small dripleaf -> fern (not waterlogged)
        if (clientVersion.isOlderThan(ClientVersion.V_1_17) && mat == SMALL_DRIPLEAF)
            return false;
        // Waterlogged rails were added in 1.17
        if (clientVersion.isOlderThan(ClientVersion.V_1_17) &&
                (mat == RAIL || mat == POWERED_RAIL || mat == ACTIVATOR_RAIL || mat == DETECTOR_RAIL))
            return false;

        return blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public static boolean isPlaceableLiquidBucket(ItemType mat) {
        return mat == ItemTypes.AXOLOTL_BUCKET || mat == ItemTypes.COD_BUCKET || mat == ItemTypes.PUFFERFISH_BUCKET
                || mat == ItemTypes.SALMON_BUCKET || mat == ItemTypes.TROPICAL_FISH_BUCKET || mat == ItemTypes.WATER_BUCKET;
    }

    public static Material transformBucketMaterial(ItemType mat) {
        if (mat == Material.LAVA_BUCKET) return ItemTypes.LAVA;
        if (isPlaceableLiquidBucket(mat)) return ItemTypes.WATER;
        return null;
    }

    // We are taking a shortcut here for the sake of speed and reducing world lookups
    // As we have already assumed that the player does not have water at this block
    // We do not have to track all the version differences in terms of looking for water
    // For 1.7-1.12 clients, it is safe to check SOLID_BLACKLIST directly
    public static boolean isSolidBlockingBlacklist(Material mat, ClientVersion ver) {
        // Thankfully Mojang has not changed this code much across versions
        // There very likely is a few lurking issues though, I've done my best but can't thoroughly compare 11 versions
        // but from a look, Mojang seems to keep this definition consistent throughout their game (thankfully)
        //
        // What I do is look at 1.8, 1.12, and 1.17 source code, and when I see a difference, I find the version
        // that added it.  I could have missed something if something was added to the blacklist in 1.9 but
        // was removed from it in 1.10 (although this is unlikely as the blacklist rarely changes)
        if (Materials.checkFlag(mat, SOLID_BLACKLIST)) return true;

        // 1.13-1.15 had banners on the blacklist - removed in 1.16, not implemented in 1.12 and below
        if (Materials.checkFlag(mat, BANNER))
            return ver.isNewerThanOrEquals(ClientVersion.V_1_13) && ver.isOlderThan(ClientVersion.V_1_16);

        return false;
    }

    public static boolean isAnvil(StateType mat) {
        return BlockTags.ANVIL.contains(mat);
    }

    public static boolean isWoodenChest(StateType mat) {
        return mat == StateTypes.CHEST || mat == StateTypes.TRAPPED_CHEST;
    }

    public static boolean isNoPlaceLiquid(StateType material) {
        return NO_PLACE_LIQUIDS.contains(material);
    }

    public static boolean isWaterIgnoringWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13) ? WATER : WATER_LEGACY);
    }
}
