package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

public interface IFabricConversionUtil {
    ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack);
    net.minecraft.network.chat.Component toNativeText(Component component);
}
