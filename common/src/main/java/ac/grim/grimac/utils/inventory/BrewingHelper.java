package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class BrewingHelper {
    public static boolean isBaseModifier(ItemType type) {
        return ItemTypes.NETHER_WART.equals(type) || ItemTypes.REDSTONE.equals(type) || ItemTypes.GLOWSTONE_DUST.equals(type)
                || ItemTypes.FERMENTED_SPIDER_EYE.equals(type) || ItemTypes.GUNPOWDER.equals(type) || ItemTypes.DRAGON_BREATH.equals(type);
    }

    public static boolean isEffectIngredient(ItemType type) {
        return ItemTypes.SUGAR.equals(type) || ItemTypes.RABBIT_FOOT.equals(type) || ItemTypes.GLISTERING_MELON_SLICE.equals(type)
                || ItemTypes.SPIDER_EYE.equals(type) || ItemTypes.PUFFERFISH.equals(type) || ItemTypes.MAGMA_CREAM.equals(type)
                || ItemTypes.GOLDEN_CARROT.equals(type) || ItemTypes.BLAZE_POWDER.equals(type) || ItemTypes.GHAST_TEAR.equals(type)
                || ItemTypes.TURTLE_HELMET.equals(type) || ItemTypes.PHANTOM_MEMBRANE.equals(type);
    }
}
