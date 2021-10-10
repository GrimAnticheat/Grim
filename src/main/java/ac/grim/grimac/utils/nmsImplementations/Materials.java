package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.util.Arrays;
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
    public static final int SWORD = 0b00000100000000000000000000000;
    public static final int CAULDRON = 0b00001000000000000000000000000;
    public static final int SHAPE_EXCEEDS_CUBE = 0b00010000000000000000000000000;
    // Warning: This is not accurate for 1.13-1.15 clients, use the method for those clients
    public static final int SOLID_BLACKLIST = 0b00100000000000000000000000000;
    public static final int BANNER = 0b01000000000000000000000000000;

    private static final Material CROSSBOW = XMaterial.CROSSBOW.parseMaterial();
    private static final Material BOW = XMaterial.BOW.parseMaterial();
    private static final Material TRIDENT = XMaterial.TRIDENT.parseMaterial();
    private static final Material SHIELD = XMaterial.SHIELD.parseMaterial();

    private static final Material LANTERN = XMaterial.LANTERN.parseMaterial();
    private static final Material SOUL_LANTERN = XMaterial.SOUL_LANTERN.parseMaterial();
    private static final Material SMALL_DRIPLEAF = XMaterial.SMALL_DRIPLEAF.parseMaterial();

    private static final Material AXOLOTL_BUCKET = XMaterial.AXOLOTL_BUCKET.parseMaterial();
    private static final Material COD_BUCKET = XMaterial.COD_BUCKET.parseMaterial();
    private static final Material LAVA_BUCKET = XMaterial.LAVA_BUCKET.parseMaterial();
    private static final Material PUFFERFISH_BUCKET = XMaterial.PUFFERFISH_BUCKET.parseMaterial();
    private static final Material SALMON_BUCKET = XMaterial.SALMON_BUCKET.parseMaterial();
    private static final Material TROPICAL_FISH_BUCKET = XMaterial.TROPICAL_FISH_BUCKET.parseMaterial();
    private static final Material WATER_BUCKET = XMaterial.WATER_BUCKET.parseMaterial();

    private static final Material ANVIL = XMaterial.ANVIL.parseMaterial();
    private static final Material CHIPPED_ANVIL = XMaterial.CHIPPED_ANVIL.parseMaterial();
    private static final Material DAMAGED_ANVIL = XMaterial.DAMAGED_ANVIL.parseMaterial();

    private static final Material CHEST = XMaterial.CHEST.parseMaterial();
    private static final Material TRAPPED_CHEST = XMaterial.TRAPPED_CHEST.parseMaterial();

    private static final Material RAIL = XMaterial.RAIL.parseMaterial();
    private static final Material ACTIVATOR_RAIL = XMaterial.ACTIVATOR_RAIL.parseMaterial();
    private static final Material DETECTOR_RAIL = XMaterial.DETECTOR_RAIL.parseMaterial();
    private static final Material POWERED_RAIL = XMaterial.POWERED_RAIL.parseMaterial();

    private static final int[] MATERIAL_FLAGS = new int[Material.values().length];
    private static final Set<Material> NO_PLACE_LIQUIDS = new HashSet<>();

    static {
        for (int i = 0; i < MATERIAL_FLAGS.length; i++) {
            Material material = Material.values()[i];

            //We use the one in BlockUtils also since we can't trust Material to include everything.
            if (material.isSolid()) {
                MATERIAL_FLAGS[i] |= SOLID;
            }
            if (material.name().endsWith("_STAIRS")) {
                MATERIAL_FLAGS[i] |= STAIRS;
            }

            if (material.name().contains("SLAB") || material.name().contains("_STEP") && !material.name().contains("LEGACY")) {
                MATERIAL_FLAGS[i] |= SLABS;
            }
        }

        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("_PLATE")).forEach(Materials::markAsNotSolid);
        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("SIGN")).forEach(Materials::markAsNotSolid);
        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("_BANNER")).forEach(Materials::markAsNotSolid);
        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("CORAL") && !xMaterial.name().contains("BLOCK")).forEach(Materials::markAsNotSolid);
        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("POTTED")).forEach(Materials::markAsSolid);
        Arrays.stream(Material.values()).sequential().filter(xMaterial -> xMaterial.name().contains("HEAD") || xMaterial.name().contains("SKULL")).forEach(Materials::markAsSolid);

        // fix some types where isSolid() returns the wrong value
        markAs(XMaterial.SLIME_BLOCK, SOLID);
        markAs(XMaterial.REPEATER, SOLID);
        markAs(XMaterial.SNOW, SOLID);
        markAs(XMaterial.ANVIL, SOLID);
        markAs(XMaterial.LILY_PAD, SOLID);
        markAs(XMaterial.FLOWER_POT, SOLID);
        markAs(XMaterial.SEA_PICKLE, SOLID);
        markAs(XMaterial.TURTLE_EGG, SOLID);
        markAs(XMaterial.CHORUS_FLOWER, SOLID);
        markAs(XMaterial.CHORUS_PLANT, SOLID);
        markAs(XMaterial.LADDER, SOLID);

        markAs(XMaterial.END_ROD, SOLID);
        markAs(XMaterial.SCAFFOLDING, SOLID);
        markAs(XMaterial.COCOA, SOLID);

        // Thanks a lot striders: optimization - don't mark as solid when striders don't exist
        // If you are unaware, striders can walk on lava
        if (XMaterial.supports(16))
            markAs(XMaterial.LAVA, SOLID);

        // 1.17 isSolid() mistakes, I think MD_5 just gave up with marking stuff as solid
        markAs(XMaterial.SCULK_SENSOR, SOLID);
        markAs(XMaterial.POWDER_SNOW, SOLID);
        markAs(XMaterial.BIG_DRIPLEAF, SOLID);
        markAs(XMaterial.AZALEA, SOLID);
        markAs(XMaterial.FLOWERING_AZALEA, SOLID);
        markAs(XMaterial.POINTED_DRIPSTONE, SOLID);


        // Lava hasn't changed, other than STATIONARY_LAVA material on 1.12- servers
        markAs(XMaterial.LAVA, LAVA);
        markAs(XMaterial.STATIONARY_LAVA, LAVA);


        // Base water, flowing on 1.12- but not on 1.13+ servers
        markAs(XMaterial.WATER, WATER);
        markAs(XMaterial.WATER, WATER_LEGACY);
        if (XMaterial.isNewVersion()) {
            markAs(XMaterial.KELP, WATER_SOURCE);
            markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE_LEGACY);
        }

        // This is not water on 1.12- players
        markAs(XMaterial.SEAGRASS, WATER);
        markAs(XMaterial.SEAGRASS, WATER_SOURCE);

        // This is not water on 1.12- players
        markAs(XMaterial.TALL_SEAGRASS, WATER);
        markAs(XMaterial.TALL_SEAGRASS, WATER_SOURCE);

        // This is not water on 1.12- players
        markAs(XMaterial.KELP, WATER);
        markAs(XMaterial.KELP, WATER_SOURCE);

        // This is not water on 1.12- players
        markAs(XMaterial.KELP_PLANT, WATER);
        markAs(XMaterial.KELP_PLANT, WATER_SOURCE);

        // This is replaced by water on 1.12- players
        markAs(XMaterial.BUBBLE_COLUMN, WATER);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_LEGACY);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE_LEGACY);

        // This is the 1.12 still water block
        markAs(XMaterial.STATIONARY_WATER, WATER);
        markAs(XMaterial.STATIONARY_WATER, WATER_LEGACY);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE_LEGACY);


        // Mark blocks as climbable
        markAs(XMaterial.LADDER, CLIMBABLE);
        markAs(XMaterial.VINE, CLIMBABLE);
        markAs(XMaterial.SCAFFOLDING, CLIMBABLE);
        markAs(XMaterial.WEEPING_VINES, CLIMBABLE);
        markAs(XMaterial.WEEPING_VINES_PLANT, CLIMBABLE);
        markAs(XMaterial.TWISTING_VINES, CLIMBABLE);
        markAs(XMaterial.TWISTING_VINES_PLANT, CLIMBABLE);
        markAs(XMaterial.CAVE_VINES, CLIMBABLE);
        markAs(XMaterial.CAVE_VINES_PLANT, CLIMBABLE);

        // Piston heads have bounding boxes that exceed their own cube
        markAs(XMaterial.PISTON_HEAD, SHAPE_EXCEEDS_CUBE);

        // The solid blacklist affects water pushing code
        // It's vanilla name is "Solid"
        // The code for this has rarely changed except with that banner oddity
        //
        // Solid has nothing to do with collision in Vanilla, unlike in Grim
        // (This is due to the Materials system in vanilla being much different from our system, as supporting
        // 11 different versions of materials is really... really... hard)
        markAs(XMaterial.END_ROD, SOLID_BLACKLIST);

        markAs(XMaterial.LADDER, SOLID_BLACKLIST);
        markAs(XMaterial.LEVER, SOLID_BLACKLIST);
        markAs(XMaterial.RAIL, SOLID_BLACKLIST);
        markAs(XMaterial.ACTIVATOR_RAIL, SOLID_BLACKLIST);
        markAs(XMaterial.DETECTOR_RAIL, SOLID_BLACKLIST);
        markAs(XMaterial.POWERED_RAIL, SOLID_BLACKLIST);
        markAs(XMaterial.REDSTONE, SOLID_BLACKLIST);
        markAs(XMaterial.REDSTONE_WIRE, SOLID_BLACKLIST);
        markAs(XMaterial.REDSTONE_TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.REPEATER, SOLID_BLACKLIST);
        markAs(XMaterial.COMPARATOR, SOLID_BLACKLIST);

        markAs(XMaterial.SCAFFOLDING, SOLID_BLACKLIST);
        // Cobwebs are their own thing in the blacklist and don't have a category, or have their own category
        markAs(XMaterial.COBWEB, SOLID_BLACKLIST);

        markLegacyAs("LEGACY_DIODE_BLOCK_OFF", SOLID_BLACKLIST);
        markLegacyAs("LEGACY_DIODE_BLOCK_ON", SOLID_BLACKLIST);
        markLegacyAs("LEGACY_REDSTONE_COMPARATOR_ON", SOLID_BLACKLIST);
        markLegacyAs("LEGACY_REDSTONE_COMPARATOR_OFF", SOLID_BLACKLIST);

        markAs(XMaterial.REDSTONE_WALL_TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.SOUL_TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.SOUL_WALL_TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.WALL_TORCH, SOLID_BLACKLIST);
        markAs(XMaterial.TRIPWIRE, SOLID_BLACKLIST);
        markAs(XMaterial.TRIPWIRE_HOOK, SOLID_BLACKLIST);
        // Exempt as snow
        markAs(XMaterial.SNOW, SOLID_BLACKLIST);
        // Transparent
        markAs(XMaterial.FIRE, SOLID_BLACKLIST);
        markAs(XMaterial.STRUCTURE_VOID, SOLID_BLACKLIST);
        // Portals are exempted
        markAs(XMaterial.NETHER_PORTAL, SOLID_BLACKLIST);
        markAs(XMaterial.END_PORTAL, SOLID_BLACKLIST);

        // This is a bit messy, but these are all the plants in 1.17 (all blacklisted for blocking movement)
        // Hopefully with PacketEvents 2.0, all the errors from replacement blocks will go away
        // (Such as a solid blacklist block going to a non-solid blacklist block)
        markAs(XMaterial.GRASS, SOLID_BLACKLIST);
        markAs(XMaterial.FERN, SOLID_BLACKLIST);
        markAs(XMaterial.DEAD_BUSH, SOLID_BLACKLIST);
        markAs(XMaterial.TALL_SEAGRASS, SOLID_BLACKLIST);
        markAs(XMaterial.DANDELION, SOLID_BLACKLIST);
        markAs(XMaterial.POPPY, SOLID_BLACKLIST);
        markAs(XMaterial.BLUE_ORCHID, SOLID_BLACKLIST);
        markAs(XMaterial.ALLIUM, SOLID_BLACKLIST);
        markAs(XMaterial.AZURE_BLUET, SOLID_BLACKLIST);
        // tulip done in loop
        markAs(XMaterial.OXEYE_DAISY, SOLID_BLACKLIST);
        markAs(XMaterial.CORNFLOWER, SOLID_BLACKLIST);
        markAs(XMaterial.WITHER_ROSE, SOLID_BLACKLIST);
        markAs(XMaterial.LILY_OF_THE_VALLEY, SOLID_BLACKLIST);
        markAs(XMaterial.BROWN_MUSHROOM, SOLID_BLACKLIST);
        markAs(XMaterial.RED_MUSHROOM, SOLID_BLACKLIST);
        markAs(XMaterial.WHEAT, SOLID_BLACKLIST);
        markAs(XMaterial.SUGAR_CANE, SOLID_BLACKLIST);
        markAs(XMaterial.VINE, SOLID_BLACKLIST);
        markAs(XMaterial.GLOW_LICHEN, SOLID_BLACKLIST);
        markAs(XMaterial.LILY_PAD, SOLID_BLACKLIST);
        markAs(XMaterial.NETHER_WART, SOLID_BLACKLIST);
        markAs(XMaterial.COCOA, SOLID_BLACKLIST);
        markAs(XMaterial.CARROTS, SOLID_BLACKLIST);
        markAs(XMaterial.POTATO, SOLID_BLACKLIST);
        markAs(XMaterial.SUNFLOWER, SOLID_BLACKLIST);
        markAs(XMaterial.LILAC, SOLID_BLACKLIST);
        markAs(XMaterial.ROSE_BUSH, SOLID_BLACKLIST);
        markAs(XMaterial.PEONY, SOLID_BLACKLIST);
        markAs(XMaterial.TALL_GRASS, SOLID_BLACKLIST);
        markAs(XMaterial.LARGE_FERN, SOLID_BLACKLIST);
        markAs(XMaterial.CHORUS_PLANT, SOLID_BLACKLIST);
        markAs(XMaterial.CHORUS_FLOWER, SOLID_BLACKLIST);
        markAs(XMaterial.BEETROOT, SOLID_BLACKLIST);
        markAs(XMaterial.KELP, SOLID_BLACKLIST);
        markAs(XMaterial.KELP_PLANT, SOLID_BLACKLIST);
        markAs(XMaterial.SEA_PICKLE, SOLID_BLACKLIST);
        markAs(XMaterial.BAMBOO, SOLID_BLACKLIST);
        markAs(XMaterial.BAMBOO_SAPLING, SOLID_BLACKLIST);
        markAs(XMaterial.SWEET_BERRY_BUSH, SOLID_BLACKLIST);
        markAs(XMaterial.WARPED_FUNGUS, SOLID_BLACKLIST);
        markAs(XMaterial.CRIMSON_FUNGUS, SOLID_BLACKLIST);
        markAs(XMaterial.WEEPING_VINES, SOLID_BLACKLIST);
        markAs(XMaterial.WEEPING_VINES_PLANT, SOLID_BLACKLIST);
        markAs(XMaterial.TWISTING_VINES, SOLID_BLACKLIST);
        markAs(XMaterial.TWISTING_VINES_PLANT, SOLID_BLACKLIST);
        markAs(XMaterial.CRIMSON_ROOTS, SOLID_BLACKLIST);
        markAs(XMaterial.CAVE_VINES, SOLID_BLACKLIST);
        markAs(XMaterial.CAVE_VINES_PLANT, SOLID_BLACKLIST);
        markAs(XMaterial.SPORE_BLOSSOM, SOLID_BLACKLIST);
        markAs(XMaterial.AZALEA, SOLID_BLACKLIST);
        markAs(XMaterial.FLOWERING_AZALEA, SOLID_BLACKLIST);
        markAs(XMaterial.MOSS_CARPET, SOLID_BLACKLIST);
        markAs(XMaterial.BIG_DRIPLEAF, SOLID_BLACKLIST);
        markAs(XMaterial.SMALL_DRIPLEAF, SOLID_BLACKLIST);
        markAs(XMaterial.HANGING_ROOTS, SOLID_BLACKLIST);

        NO_PLACE_LIQUIDS.add(XMaterial.WATER.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.LAVA.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.STATIONARY_WATER.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.STATIONARY_LAVA.parseMaterial());

        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_SWORD")) MATERIAL_FLAGS[mat.ordinal()] |= SWORD;
            if (!mat.isBlock()) continue;
            if (checkFlag(mat, LAVA)) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("FENCE") && !mat.name().equalsIgnoreCase("IRON_FENCE")) {
                MATERIAL_FLAGS[mat.ordinal()] |= SHAPE_EXCEEDS_CUBE;
                if (!mat.name().contains("GATE")) MATERIAL_FLAGS[mat.ordinal()] |= FENCE;
                else {
                    MATERIAL_FLAGS[mat.ordinal()] |= GATE;
                    // Client side changes gate immediately
                    MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
                }
            }
            if (mat.name().contains("WALL") && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER") &&
                    !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH")) {
                MATERIAL_FLAGS[mat.ordinal()] |= SHAPE_EXCEEDS_CUBE;
                MATERIAL_FLAGS[mat.ordinal()] |= WALL;
            }
            if (mat.name().contains("BED") && !mat.name().contains("ROCK")) MATERIAL_FLAGS[mat.ordinal()] |= BED;
            if (mat.name().contains("ICE")) MATERIAL_FLAGS[mat.ordinal()] |= ICE_BLOCKS;
            if (mat.name().contains("CARPET")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().endsWith("_GATE")) MATERIAL_FLAGS[mat.ordinal()] |= GATE;
            if (mat.name().endsWith("AIR")) MATERIAL_FLAGS[mat.ordinal()] |= AIR;
            if (mat.name().endsWith("AIR")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("TRAPDOOR") || mat.name().contains("TRAP_DOOR")) {
                MATERIAL_FLAGS[mat.ordinal()] |= TRAPDOOR;
                if (!mat.name().contains("IRON"))
                    MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }

            if (mat.name().contains("_BANNER")) MATERIAL_FLAGS[mat.ordinal()] |= BANNER;
            if (mat.name().contains("LEAVES")) MATERIAL_FLAGS[mat.ordinal()] |= LEAVES;
            if (mat.name().contains("DIODE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("COMPARATOR")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("_DOOR")) MATERIAL_FLAGS[mat.ordinal()] |= DOOR;
            if (mat.name().contains("_DOOR") && !mat.name().contains("IRON"))
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            if (mat.name().contains("SHULKER_BOX")) MATERIAL_FLAGS[mat.ordinal()] |= SHULKER;
            if (mat.name().contains("GLASS") && !mat.name().contains("PANE") && !mat.name().contains("THIN_GLASS"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_BLOCK;
            // THIN_GLASS and IRON_FENCE are 1.8 names for these materials
            if ((mat.name().contains("GLASS") && mat.name().contains("PANE")) || mat.name().contains("THIN_GLASS") || mat.name().contains("IRON_FENCE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_PANE;
            if (mat.name().contains("SKULL") || mat.name().contains("HEAD"))
                MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("_SIGN")) markAsNotSolid(mat);
            if (mat.name().contains("BUTTON")) MATERIAL_FLAGS[mat.ordinal()] |= BUTTON;
            if (mat.name().contains("CANDLE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            // 1.17 separates the types of cauldrons
            if (mat.name().contains("CAULDRON")) MATERIAL_FLAGS[mat.ordinal()] |= CAULDRON;
            if (mat.name().contains("BUTTON")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("SKULL") || mat.name().contains("HEAD"))
                MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("CARPET")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("SAPLING")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("TULIP")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("STEM")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("SEED")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("CORAL") && !mat.name().contains("DEAD") && !mat.name().contains("WALL"))
                MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("POTTED")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
            if (mat.name().contains("CANDLE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID_BLACKLIST;
        }
    }

    private static void markAsNotSolid(Material material) {
        // Remove the least significant bit
        MATERIAL_FLAGS[material.ordinal()] &= Integer.MAX_VALUE - 1;
    }

    private static void markAs(XMaterial material, int flag) {
        // Set the flag only if the version has the material
        if (material.parseMaterial() != null) {
            MATERIAL_FLAGS[material.parseMaterial().ordinal()] |= flag;
        }
    }

    private static void markLegacyAs(String material, int flag) {
        Material mat = matchLegacy(material);
        // Set the flag only if the version has the material
        if (mat != null) {
            MATERIAL_FLAGS[mat.ordinal()] |= flag;
        }
    }

    public static Material matchLegacy(String material) {
        if (XMaterial.isNewVersion()) {
            return null;
        }
        return Material.getMaterial(material.replace("LEGACY_", ""));
    }

    private static void markAsSolid(Material material) {
        // Set the flag only if the version has the material
        MATERIAL_FLAGS[material.ordinal()] |= Materials.SOLID;
    }

    public static int getBitmask(Material material) {
        return MATERIAL_FLAGS[material.ordinal()];
    }

    public static boolean isUsable(Material material) {
        return material != null && (material.isEdible() || material == Material.POTION || material == Material.MILK_BUCKET
                || material == CROSSBOW || material == BOW || checkFlag(material, SWORD)
                || material == TRIDENT || material == SHIELD);
    }

    public static boolean checkFlag(Material material, int flag) {
        return (MATERIAL_FLAGS[material.ordinal()] & flag) == flag;
    }

    public static boolean isWater(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.v_1_13) ? WATER : WATER_LEGACY) || isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
        if (clientVersion.isOlderThanOrEquals(ClientVersion.v_1_12_2)) return false;
        if (!XMaterial.isNewVersion()) return false;

        FlatBlockState flat = (FlatBlockState) state;
        BlockData blockData = flat.getBlockData();
        Material mat = blockData.getMaterial();

        // Waterlogged lanterns were added in 1.16.2
        if (clientVersion.isOlderThan(ClientVersion.v_1_16_2) && (mat == LANTERN || mat == SOUL_LANTERN))
            return false;
        // ViaVersion small dripleaf -> fern (not waterlogged)
        if (clientVersion.isOlderThan(ClientVersion.v_1_17) && mat == SMALL_DRIPLEAF)
            return false;
        // Waterlogged rails were added in 1.17
        if (clientVersion.isOlderThan(ClientVersion.v_1_17) &&
                (mat == RAIL || mat == POWERED_RAIL || mat == ACTIVATOR_RAIL || mat == DETECTOR_RAIL))
            return false;

        return blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public static boolean isPlaceableLiquidBucket(Material mat) {
        return mat == AXOLOTL_BUCKET || mat == COD_BUCKET || mat == LAVA_BUCKET || mat == PUFFERFISH_BUCKET
                || mat == SALMON_BUCKET || mat == TROPICAL_FISH_BUCKET || mat == WATER_BUCKET;
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
            return ver.isNewerThanOrEquals(ClientVersion.v_1_13) && ver.isOlderThan(ClientVersion.v_1_16);

        return false;
    }

    public static boolean isAnvil(Material mat) {
        return mat == ANVIL || mat == CHIPPED_ANVIL || mat == DAMAGED_ANVIL;
    }

    public static boolean isWoodenChest(Material mat) {
        return mat == CHEST || mat == TRAPPED_CHEST;
    }

    public static boolean isNoPlaceLiquid(Material material) {
        return NO_PLACE_LIQUIDS.contains(material);
    }

    public static boolean isWaterIgnoringWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.v_1_13) ? WATER : WATER_LEGACY);
    }
}
