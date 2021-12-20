package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentHelper {
    public static boolean hasBindingCurse(ItemStack itemstack) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_11)) return false;
        return itemstack.getStack().containsEnchantment(Enchantment.BINDING_CURSE);
    }
}
