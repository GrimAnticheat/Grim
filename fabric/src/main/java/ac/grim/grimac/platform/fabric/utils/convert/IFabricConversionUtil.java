package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;
import net.minecraft.text.Text;

public interface IFabricConversionUtil {
    ItemStack fromFabricItemStack(net.minecraft.item.ItemStack fabricStack);
    Text toNativeText(Component component);
}
