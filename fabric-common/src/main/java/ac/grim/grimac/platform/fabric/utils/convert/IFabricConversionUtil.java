package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

// NMS-free so the shared AbstractFabricPlatformInventory (fabric-common) can reference it.
// The native arguments/returns are typed Object because this module forbids net.minecraft.*;
// the per-version implementors (Fabric<ver>ConversionUtil) cast back to the NMS ItemStack /
// network.chat.Component. PacketEvents ItemStack and adventure Component are JDK-adjacent
// library types that fabric-common is allowed to use (see fabric-common/build.gradle.kts).
public interface IFabricConversionUtil {

    /** @param fabricItemStack the native {@code net.minecraft.world.item.ItemStack} */
    ItemStack fromFabricItemStack(Object fabricItemStack);

    /** @return the native {@code net.minecraft.network.chat.Component} for the adventure component */
    Object toNativeText(Component component);
}
