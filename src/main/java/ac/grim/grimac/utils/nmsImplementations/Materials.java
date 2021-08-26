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
    public static final int ICE = 0b00000000000000000000100000000;
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

        NO_PLACE_LIQUIDS.add(XMaterial.WATER.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.LAVA.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.STATIONARY_WATER.parseMaterial());
        NO_PLACE_LIQUIDS.add(XMaterial.STATIONARY_LAVA.parseMaterial());

        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_SWORD")) MATERIAL_FLAGS[mat.ordinal()] |= SWORD;
            if (!mat.isBlock()) continue;
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
            if (mat.name().contains("ICE")) MATERIAL_FLAGS[mat.ordinal()] |= ICE;
            if (mat.name().contains("CARPET")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().endsWith("_GATE")) MATERIAL_FLAGS[mat.ordinal()] |= GATE;
            if (mat.name().endsWith("AIR")) MATERIAL_FLAGS[mat.ordinal()] |= AIR;
            if (mat.name().contains("TRAPDOOR") || mat.name().contains("TRAP_DOOR")) {
                MATERIAL_FLAGS[mat.ordinal()] |= TRAPDOOR;
                if (!mat.name().contains("IRON"))
                    MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            }

            if (mat.name().contains("LEAVES")) MATERIAL_FLAGS[mat.ordinal()] |= LEAVES;
            if (mat.name().contains("DIODE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("COMPARATOR")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("_DOOR")) MATERIAL_FLAGS[mat.ordinal()] |= DOOR;
            if (mat.name().contains("_DOOR") && !mat.name().contains("IRON"))
                MATERIAL_FLAGS[mat.ordinal()] |= CLIENT_SIDE_INTERACTABLE;
            if (mat.name().contains("SHULKER_BOX")) MATERIAL_FLAGS[mat.ordinal()] |= SHULKER;
            if (mat.name().contains("GLASS") && !mat.name().contains("PANE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_BLOCK;
            if (mat.name().contains("GLASS") && mat.name().contains("PANE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_PANE;
            if (mat.name().contains("SKULL") || mat.name().contains("HEAD"))
                MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("_SIGN")) markAsNotSolid(mat);
            if (mat.name().contains("BUTTON")) MATERIAL_FLAGS[mat.ordinal()] |= BUTTON;
            if (mat.name().contains("CANDLE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            // 1.17 separates the types of cauldrons
            if (mat.name().contains("CAULDRON")) MATERIAL_FLAGS[mat.ordinal()] |= CAULDRON;
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

    public static boolean isWaterFlat(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.v_1_13) ? WATER : WATER_LEGACY) || isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
        if (clientVersion.isOlderThanOrEquals(ClientVersion.v_1_12_2)) return false;
        if (!XMaterial.isNewVersion()) return false;

        FlatBlockState flat = (FlatBlockState) state;
        BlockData blockData = flat.getBlockData();

        // Waterlogged lanterns were added in 1.16.2
        if (clientVersion.isOlderThan(ClientVersion.v_1_16_2) && (blockData.getMaterial() == LANTERN || blockData.getMaterial() == SOUL_LANTERN))
            return false;
        // ViaVersion small dripleaf -> fern (not waterlogged)
        if (clientVersion.isOlderThan(ClientVersion.v_1_17) && blockData.getMaterial() == SMALL_DRIPLEAF)
            return false;

        return blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public static boolean isPlaceableLiquidBucket(Material mat) {
        return mat == AXOLOTL_BUCKET || mat == COD_BUCKET || mat == LAVA_BUCKET || mat == PUFFERFISH_BUCKET
                || mat == SALMON_BUCKET || mat == TROPICAL_FISH_BUCKET || mat == WATER_BUCKET;
    }

    public static boolean isNoPlaceLiquid(Material material) {
        return NO_PLACE_LIQUIDS.contains(material);
    }

    public static boolean isWaterMagic(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), clientVersion.isNewerThanOrEquals(ClientVersion.v_1_13) ? WATER : WATER_LEGACY);
    }

    public static Material matchLegacy(String material) {
        if (XMaterial.isNewVersion()) {
            return null;
        }
        return Material.getMaterial(material.replace("LEGACY_", ""));
    }
}
