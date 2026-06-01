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

// 26.X conversion. Same shape as Fabric1205ConversionUtil — ItemStack.STREAM_CODEC
// (encode → PE packet wrapper → readItemStack) survived the 1.21.11 → 26.1.2
// transition unchanged.
public class Fabric261ConversionUtil implements IFabricConversionUtil {
    @Override
    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
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
    public net.minecraft.network.chat.Component toNativeText(Component component) {
        // 26.X removed Component.Serializer in favor of ComponentSerialization.CODEC
        // with the DFU JsonOps path, which would need server registry context to
        // round-trip styled adventure components properly. For alerts + console
        // messages (the only callers today) plain-text flatten is good enough —
        // proper styled conversion lands when an adventure-platform-fabric build
        // ships for 26.X.
        StringBuilder out = new StringBuilder();
        ComponentFlattener.basic().flatten(component, out::append);
        return net.minecraft.network.chat.Component.literal(out.toString());
    }
}
