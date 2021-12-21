package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.util.HashSet;
import java.util.Set;

public class Materials {
    public static final int SOLID = 0b00000000000000000000000000001;
    public static final int CLIMBABLE = 0b00000000000000000000000000010;

    public static final int WALL = 0b00000000000000000000000000100;
    public static final int STAIRS = 0b00000000000000000000000001000;
    public static final int SLABS = 0b00000000000000000000000010000;

    public static final int WATER = 0b00000000000000000000000100000;
    public static final int LAVA = 0b00000000000000000000001000000;

    public static final int BUTTON = 0b00000000000000000000010000000;

    public static final int ICE_BLOCKS = 0b00000000000000000000100000000;

    public static final int FENCE = 0b00000000000000000001000000000;
    public static final int GATE = 0b00000000000000000010000000000;
    public static final int BED = 0b00000000000000000100000000000;
    public static final int AIR = 0b00000000000000001000000000000;

    public static final int TRAPDOOR = 0b00000000000000010000000000000;

    public static final int WATER_SOURCE = 0b00000000000000100000000000000;

    public static final int LEAVES = 0b00000000000001000000000000000;
    public static final int DOOR = 0b00000000000010000000000000000;
    public static final int SHULKER = 0b00000000000100000000000000000;

    public static final int GLASS_BLOCK = 0b00000000001000000000000000000;
    public static final int GLASS_PANE = 0b00000000010000000000000000000;

    public static final int WATER_LEGACY = 0b00000000100000000000000000000;
    public static final int WATER_SOURCE_LEGACY = 0b00000001000000000000000000000;
    public static final int CLIENT_SIDE_INTERACTABLE = 0b00000010000000000000000000000;
    public static final int PLANT = 0b00000100000000000000000000000;
    public static final int CAULDRON = 0b00001000000000000000000000000;
    public static final int SHAPE_EXCEEDS_CUBE = 0b00010000000000000000000000000;

    // Warning: This is not accurate for 1.13-1.15 clients, use the method for those clients
    public static final int SOLID_BLACKLIST = 0b00100000000000000000000000000;
    public static final int BANNER = 0b01000000000000000000000000000;
    // What blocks can new blocks simply replace entirely when placing?
    public static final int REPLACEABLE = 0b10000000000000000000000000000;

    private static final Set<StateType> NO_PLACE_LIQUIDS = new HashSet<>();
    private static final Set<StateType> GLASS_BLOCKS = new HashSet<>();
    private static final Set<StateType> GLASS_PANES = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS_LEGACY = new HashSet<>();
    private static final Set<StateType> LAVA_LIQUIDS = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES_LEGACY = new HashSet<>();

    private static final Set<StateType> CLIENT_SIDE = new HashSet<>();


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

        for (Material mat : Material.values()) {
            if (!mat.isBlock()) continue;

            if (mat.name().contains("FENCE") && !mat.name().equalsIgnoreCase("IRON_FENCE")) {
                if (!mat.name().contains("GATE")) MATERIAL_FLAGS[mat.ordinal()] |= FENCE;
                else {
                    MATERIAL_FLAGS[mat.ordinal()] |= GATE;
                    // Client side changes gate immediately
                    MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
                }
            }

            BlockTags.FLOWER_POTS

            if (mat.name().contains("ANVIL")) {
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }
            if (mat.name().contains("BED")) {
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }
            if (mat.name().contains("BUTTON")) {
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }
            if (mat.name().contains("SHULKER")) {
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }
            if (mat.name().contains("SIGN")) {
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }
            if (mat.name().contains("POTTED")) MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;

            if (mat.name().contains("ICE")) MATERIAL_FLAGS[mat.ordinal()] |= ICE_BLOCKS;
            if (mat.name().endsWith("_GATE")) MATERIAL_FLAGS[mat.ordinal()] |= GATE;

            if (mat.name().contains("TRAPDOOR") || mat.name().contains("TRAP_DOOR")) {
                if (!mat.name().contains("IRON"))
                    MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }

            if (mat.name().contains("_BANNER")) MATERIAL_FLAGS[mat.ordinal()] |= BANNER;
            if (mat.name().contains("_DOOR") && !mat.name().contains("IRON"))
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            if (mat.name().contains("GLASS") && !mat.name().contains("PANE") && !mat.name().contains("THIN_GLASS"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_BLOCK;
            // THIN_GLASS and IRON_FENCE are 1.8 names for these materials
            if ((mat.name().contains("GLASS") && mat.name().contains("PANE")) || mat.name().contains("THIN_GLASS") || mat.name().contains("IRON_FENCE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_PANE;
            if (mat.name().contains("_SIGN")) markAsNotSolid(mat);
            // 1.17 separates the types of cauldrons
            if (mat.name().contains("CAULDRON")) MATERIAL_FLAGS[mat.ordinal()] |= CAULDRON;
        }
    }

    public static boolean checkStairs(StateType type) {
        return BlockTags.STAIRS.contains(type);
    }

    public static boolean checkSlabs(StateType type) {
        return BlockTags.SLABS.contains(type);
    }

    public static boolean checkWall(StateType type) {
        return BlockTags.WALLS.contains(type);
    }

    public static boolean checkButton(StateType type) {
        return BlockTags.BUTTONS.contains(type);
    }

    public static boolean checkFence(StateType type) {
        return BlockTags.FENCES.contains(type);
    }

    public static boolean checkGate(StateType type) {
        return BlockTags.FENCE_GATES.contains(type);
    }

    public static boolean checkBed(StateType type) {
        return BlockTags.BEDS.contains(type);
    }

    public static boolean checkAir(StateType type) {
        return type.isAir();
    }

    public static boolean checkLeaves(StateType type) {
        return BlockTags.LEAVES.contains(type);
    }

    public static boolean checkDoor(StateType type) {
        return BlockTags.DOORS.contains(type);
    }

    public static boolean checkShulker(StateType type) {
        return BlockTags.SHULKER_BOXES.contains(type);
    }

    public static boolean checkGlassBlock(StateType type) {

    }

    public static boolean checkGlassPane(StateType type) {

    }

    public static boolean checkClimable(StateType type) {
        return BlockTags.CLIMBABLE.contains(type);
    }

    public static boolean checkCauldron(StateType type) {
        return BlockTags.CAULDRONS.contains(type);
    }

    private static void markAsNotSolid(Material material) {
        // Remove the least significant bit
        MATERIAL_FLAGS[material.ordinal()] &= Integer.MAX_VALUE - 1;
    }

    private static void markAs(StateTypes material, int flag) {
        // Set the flag only if the version has the material
        if (material != null) {
            MATERIAL_FLAGS[material.ordinal()] |= flag;
        }
    }

    public static boolean isUsable(ItemType material) {
        return material != null && (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                || material == ItemTypes.CROSSBOW || material == ItemTypes.BOW || material.toString().endsWith("SWORD")
                || material == ItemTypes.TRIDENT || material == ItemTypes.SHIELD);
    }

    public static boolean checkFlag(Material material, int flag) {
        return (MATERIAL_FLAGS[material.ordinal()] & flag) == flag;
    }

    public static boolean isWater(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13) ? WATER : WATER_LEGACY) || isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
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
