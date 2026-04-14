package ac.grim.grimac.platform.fabric.mc1161.util.convert;

import ac.grim.grimac.platform.fabric.utils.convert.FabricItemStackConversion;
import ac.grim.grimac.platform.fabric.utils.convert.FabricTextConversion;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

public class Fabric1140ConversionUtil implements IFabricConversionUtil {
    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
        return FabricItemStackConversion.peItemStackFromNative(fabricStack);
    }

    public net.minecraft.network.chat.Component toNativeText(Component component) {
        return FabricTextConversion.parseAdventureToNative(component);
    }
}
