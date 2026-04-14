package ac.grim.grimac.platform.fabric.utils.convert;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Shared native ItemStack → PacketEvents item conversion (STREAM_CODEC + PacketWrapper).
 */
public final class FabricItemStackConversion {
    private FabricItemStackConversion() {}

    public static ItemStack peItemStackFromNative(net.minecraft.world.item.ItemStack fabricStack) {
        if (fabricStack == null || fabricStack.isEmpty()) {
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
            LogUtil.error("Failed to encode ItemStack: " + fabricStack, e);
            return ItemStack.EMPTY;
        } finally {
            ByteBufHelper.release(buffer);
        }
    }
}
