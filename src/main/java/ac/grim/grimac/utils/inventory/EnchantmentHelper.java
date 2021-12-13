package ac.grim.grimac.utils.inventory;

import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentHelper {
    public static boolean hasBindingCurse(WrappedStack itemstack) {
        if (ServerVersion.getVersion().isOlderThan(ServerVersion.v_1_11)) return false;
        return itemstack.getStack().containsEnchantment(Enchantment.BINDING_CURSE);
    }
}
