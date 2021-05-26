package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Lantern;

import java.util.Arrays;

public class Materials {
    public static final int SOLID = 0b00000000000000000000000000001;
    public static final int CLIMBABLE = 0b00000000000000000000000000010;
    public static final int WALL = 0b00000000000000000000000000100;
    public static final int STAIRS = 0b00000000000000000000000001000;
    public static final int SLABS = 0b00000000000000000000000010000;
    public static final int WATER = 0b00000000000000000000000100000;
    public static final int LAVA = 0b00000000000000000000001000000;
    public static final int LIQUID = 0b00000000000000000000010000000;
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
    private static final int[] MATERIAL_FLAGS = new int[Material.values().length];

    static {
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("_PLATE")).forEach(Materials::markAsNotSolid);
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("_SIGN")).forEach(Materials::markAsNotSolid);
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("_BANNER")).forEach(Materials::markAsNotSolid);
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("CORAL") && !xMaterial.name().contains("BLOCK")).forEach(Materials::markAsNotSolid);
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("POTTED")).forEach(material -> markAs(material, SOLID));
        Arrays.stream(XMaterial.values()).sequential().filter(xMaterial -> xMaterial.name().contains("HEAD") || xMaterial.name().contains("SKULL")).forEach(material -> markAs(material, SOLID));

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

        markAs(XMaterial.WATER, WATER);

        markAs(XMaterial.SEAGRASS, WATER);
        markAs(XMaterial.SEAGRASS, WATER_SOURCE);

        markAs(XMaterial.TALL_SEAGRASS, WATER);
        markAs(XMaterial.TALL_SEAGRASS, WATER_SOURCE);

        markAs(XMaterial.KELP, WATER);
        markAs(XMaterial.KELP, WATER_SOURCE);

        markAs(XMaterial.KELP_PLANT, WATER);
        markAs(XMaterial.KELP_PLANT, WATER_SOURCE);

        markAs(XMaterial.BUBBLE_COLUMN, WATER);
        markAs(XMaterial.BUBBLE_COLUMN, WATER_SOURCE);

        markAs(XMaterial.LAVA, LAVA);

        Material legacyStationaryWater = matchLegacy("STATIONARY_WATER");
        if (legacyStationaryWater != null) {
            MATERIAL_FLAGS[legacyStationaryWater.ordinal()] = WATER;
        }

        Material legacyStationaryLava = matchLegacy("STATIONARY_LAVA");
        if (legacyStationaryLava != null) {
            MATERIAL_FLAGS[legacyStationaryLava.ordinal()] = LAVA;
        }

        markAs(XMaterial.LADDER, CLIMBABLE);
        markAs(XMaterial.VINE, CLIMBABLE);
        markAs(XMaterial.SCAFFOLDING, CLIMBABLE);
        markAs(XMaterial.WEEPING_VINES, CLIMBABLE);
        markAs(XMaterial.WEEPING_VINES_PLANT, CLIMBABLE);
        markAs(XMaterial.TWISTING_VINES, CLIMBABLE);
        markAs(XMaterial.TWISTING_VINES_PLANT, CLIMBABLE);

        for (Material mat : Material.values()) {
            if (!mat.isBlock()) continue;
            if (mat.name().contains("FENCE")) {
                if (!mat.name().contains("GATE")) MATERIAL_FLAGS[mat.ordinal()] |= FENCE;
                else MATERIAL_FLAGS[mat.ordinal()] |= GATE;
            }
            if (mat.name().contains("WALL") && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER") &&
                    !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
                MATERIAL_FLAGS[mat.ordinal()] |= WALL;
            if (mat.name().contains("BED") && !mat.name().contains("ROCK")) MATERIAL_FLAGS[mat.ordinal()] |= BED;
            if (mat.name().contains("ICE")) MATERIAL_FLAGS[mat.ordinal()] |= ICE;
            if (mat.name().contains("CARPET")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().endsWith("_GATE")) MATERIAL_FLAGS[mat.ordinal()] |= GATE;
            if (mat.name().endsWith("AIR")) MATERIAL_FLAGS[mat.ordinal()] |= AIR;
            if (mat.name().contains("TRAPDOOR") || mat.name().contains("TRAP_DOOR"))
                MATERIAL_FLAGS[mat.ordinal()] |= TRAPDOOR;
            if (mat.name().contains("LEAVES")) MATERIAL_FLAGS[mat.ordinal()] |= LEAVES;
            if (mat.name().contains("DIODE")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("COMPARATOR")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            if (mat.name().contains("_DOOR")) MATERIAL_FLAGS[mat.ordinal()] |= DOOR;
            if (mat.name().contains("SHULKER_BOX")) MATERIAL_FLAGS[mat.ordinal()] |= SHULKER;
            if (mat.name().contains("GLASS") && !mat.name().contains("PANE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_BLOCK;
            if (mat.name().contains("GLASS") && mat.name().contains("PANE"))
                MATERIAL_FLAGS[mat.ordinal()] |= GLASS_PANE;
            if (mat.name().contains("SKULL") || mat.name().contains("HEAD"))
                MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
        }
    }

    private static void markAsNotSolid(XMaterial material) {
        // Set the flag only if the version has the material
        if (material.parseMaterial() != null) {
            MATERIAL_FLAGS[material.parseMaterial().ordinal()] = 0;
        }
    }

    private static void markAs(XMaterial material, int flag) {
        // Set the flag only if the version has the material
        if (material.parseMaterial() != null) {
            MATERIAL_FLAGS[material.parseMaterial().ordinal()] |= flag;
        }
    }

    public static int getBitmask(Material material) {
        return MATERIAL_FLAGS[material.ordinal()];
    }

    public static boolean checkFlag(Material material, int flag) {
        return (MATERIAL_FLAGS[material.ordinal()] & flag) == flag;
    }

    public static boolean isUsable(Material material) {
        String nameLower = material.name().toLowerCase();
        return material.isEdible()
                || nameLower.contains("bow")
                || nameLower.contains("sword")
                || nameLower.contains("trident");
    }

    public static boolean isWater(ClientVersion clientVersion, BaseBlockState state) {
        return checkFlag(state.getMaterial(), WATER) || isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, BaseBlockState state) {
        if (clientVersion.isOlderThanOrEquals(ClientVersion.v_1_12_2)) return false;
        if (state instanceof MagicBlockState) return false;

        FlatBlockState flat = (FlatBlockState) state;
        BlockData blockData = flat.getBlockData();

        // Waterlogged lanterns were added in 1.16.2
        if (clientVersion.isOlderThan(ClientVersion.v_1_16_2) && blockData instanceof Lantern) return false;

        return blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public static Material matchLegacy(String material) {
        if (XMaterial.isNewVersion()) {
            return null;
        }
        return Material.getMaterial(material.replace("LEGACY_", ""));
    }
}
