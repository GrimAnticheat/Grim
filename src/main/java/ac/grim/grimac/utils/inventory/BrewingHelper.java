package ac.grim.grimac.utils.inventory;

import org.bukkit.Material;

public class BrewingHelper {
    public static boolean isBaseModifier(Material type) {
        switch (type) {
            case NETHER_WART:
            case REDSTONE:
            case GLOWSTONE_DUST:
            case FERMENTED_SPIDER_EYE:
            case GUNPOWDER:
            case DRAGON_BREATH:
                return true;
            default:
                return false;
        }
    }

    public static boolean isEffectIngredient(Material type) {
        switch (type) {
            case SUGAR:
            case RABBIT_FOOT:
            case GLISTERING_MELON_SLICE:
            case SPIDER_EYE:
            case PUFFERFISH:
            case MAGMA_CREAM:
            case GOLDEN_CARROT:
            case BLAZE_POWDER:
            case GHAST_TEAR:
            case TURTLE_HELMET:
            case PHANTOM_MEMBRANE:
                return true;
            default:
                return false;
        }
    }
}
