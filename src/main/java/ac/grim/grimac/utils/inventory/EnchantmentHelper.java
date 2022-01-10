package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentType;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;

public class EnchantmentHelper {
    public static boolean isCurse(EnchantmentType type) {
        return type == EnchantmentTypes.BINDING_CURSE || type == EnchantmentTypes.VANISHING_CURSE;
    }
}
