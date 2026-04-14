package ac.grim.grimac.platform.fabric.mc1205.convert;

import ac.grim.grimac.platform.fabric.utils.convert.FabricItemStackConversion;
import ac.grim.grimac.platform.fabric.utils.convert.FabricTextConversion;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

public class Fabric1205ConversionUtil implements IFabricConversionUtil {
    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
        return FabricItemStackConversion.peItemStackFromNative(fabricStack);
    }

    /**
     * Codec parse uses JsonOps only; registry-heavy component features may be limited until full
     * registry-backed parse is wired for this path.
     */
    public net.minecraft.network.chat.Component toNativeText(Component component) {
        return FabricTextConversion.parseAdventureToNative(component);
    }
}
