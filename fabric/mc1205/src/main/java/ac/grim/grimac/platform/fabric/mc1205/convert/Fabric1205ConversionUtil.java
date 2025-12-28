package ac.grim.grimac.platform.fabric.mc1205.convert;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class Fabric1205ConversionUtil implements IFabricConversionUtil {
    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
        if (fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Allocate a ByteBuf
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            // Obtain the DynamicRegistryManager (you need to provide this from your context)
            RegistryAccess registryManager = GrimACFabricLoaderPlugin.FABRIC_SERVER.registryAccess(); // Replace with actual method to get registry manager

            // Create a RegistryByteBuf
            RegistryFriendlyByteBuf registryByteBuf = new RegistryFriendlyByteBuf(buffer, registryManager);

            // Encode the ItemStack using the appropriate PacketCodec
            net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(registryByteBuf, fabricStack);

            // Create a PacketWrapper to read the ItemStack back (if needed)
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer);
            return wrapper.readItemStack();
        } catch (Exception e) {
            // Handle encoding errors
            LogUtil.error("Failed to encode ItemStack: {}" + fabricStack, e);
            return ItemStack.EMPTY;
        } finally {
            // Release the ByteBuf to prevent memory leaks
            ByteBufHelper.release(buffer);
        }
    }

    // TODO proper registry support?
    public net.minecraft.network.chat.Component toNativeText(Component component) {
        return net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(component), RegistryAccess.EMPTY);
    }
}
