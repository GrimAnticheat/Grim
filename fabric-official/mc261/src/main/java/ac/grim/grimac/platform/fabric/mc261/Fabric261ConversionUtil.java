package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

// 26.X conversion. Same shape as Fabric1205ConversionUtil: ItemStack.STREAM_CODEC
// (encode -> PE packet wrapper -> readItemStack) survived the 1.21.11 -> 26.1.2
// transition unchanged.
public class Fabric261ConversionUtil implements IFabricConversionUtil {
    @Override
    public ItemStack fromFabricItemStack(Object fabricItemStack) {
        // NMS-free interface (fabric-common) hands the native stack as Object; cast it back.
        net.minecraft.world.item.ItemStack fabricStack = (net.minecraft.world.item.ItemStack) fabricItemStack;
        if (fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            RegistryAccess registryManager = GrimACFabricLoaderPlugin.FABRIC_SERVER.registryAccess();
            RegistryFriendlyByteBuf registryByteBuf = new RegistryFriendlyByteBuf(buffer, registryManager);
            net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(registryByteBuf, fabricStack);

            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer);
            return wrapper.readItemStack();
        } catch (Exception e) {
            LogUtil.error("Failed to encode ItemStack: {}" + fabricStack, e);
            return ItemStack.EMPTY;
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    @Override
    public Object toNativeText(Component component) {
        // PLAIN-TEXT ONLY: ComponentFlattener.basic() drops all styling/colours/events,
        // emitting a single unstyled literal. Adequate for the only callers (alerts +
        // console); full styled conversion is pending a multiversion adventure-platform
        // -fabric build for 26.X (would otherwise need server registry context to
        // round-trip via ComponentSerialization.CODEC). Same limitation as the
        // intermediary FabricSenderFactory flatten path.
        StringBuilder out = new StringBuilder();
        ComponentFlattener.basic().flatten(component, out::append);
        return net.minecraft.network.chat.Component.literal(out.toString());
    }
}
