package ac.grim.grimac.utils.inventory;

import ac.grim.grimac.utils.latency.CompensatedInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentType;

public class EnchantmentHelper {
    // Some enchants work on any armor piece but only the maximum level counts
    public static int getMaximumEnchantLevel(CompensatedInventory inventory, EnchantmentType enchantmentType) {
        int maxEnchantLevel = 0;

        ItemStack helmet = inventory.getHelmet();
        if (helmet != ItemStack.EMPTY) {
            maxEnchantLevel = Math.max(maxEnchantLevel, helmet.getEnchantmentLevel(enchantmentType));
        }

        ItemStack chestplate = inventory.getChestplate();
        if (chestplate != ItemStack.EMPTY) {
            maxEnchantLevel = Math.max(maxEnchantLevel, chestplate.getEnchantmentLevel(enchantmentType));
        }

        ItemStack leggings = inventory.getLeggings();
        if (leggings != ItemStack.EMPTY) {
            maxEnchantLevel = Math.max(maxEnchantLevel, leggings.getEnchantmentLevel(enchantmentType));
        }

        ItemStack boots = inventory.getBoots();
        if (boots != ItemStack.EMPTY) {
            maxEnchantLevel = Math.max(maxEnchantLevel, boots.getEnchantmentLevel(enchantmentType));
        }

        return maxEnchantLevel;
    }
}
