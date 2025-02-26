package ac.grim.grimac.platform.fabric.utils.convert;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import ac.grim.grimac.platform.api.player.GameMode;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;

public class FabricConversionUtil {

    public static ItemStack fromFabricItemStack(net.minecraft.item.ItemStack fabricStack) {
        if (fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Allocate a ByteBuf
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            // Obtain the DynamicRegistryManager (you need to provide this from your context)
            DynamicRegistryManager registryManager = getDynamicRegistryManager(); // Replace with actual method to get registry manager

            // Create a RegistryByteBuf
            RegistryByteBuf registryByteBuf = new RegistryByteBuf(buffer, registryManager);

            // Encode the ItemStack using the appropriate PacketCodec
            net.minecraft.item.ItemStack.PACKET_CODEC.encode(registryByteBuf, fabricStack);

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

    // Placeholder method - replace with actual way to obtain DynamicRegistryManager
    private static DynamicRegistryManager getDynamicRegistryManager() {
        // Example: Obtain from server or client context
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getRegistryManager();
        // For client: MinecraftClient.getInstance().getNetworkHandler().getRegistryManager()
//        throw new UnsupportedOperationException("DynamicRegistryManager not provided");
    }

    public static Text toNativeText(Component component) {
        return Text.Serialization.fromJsonTree(GsonComponentSerializer.gson().serializeToTree(component), DynamicRegistryManager.EMPTY);
    }

    public static net.minecraft.world.GameMode toFabricGameMode(GameMode gameMode) {
        switch (gameMode) {
            case CREATIVE:
                return net.minecraft.world.GameMode.CREATIVE;
            case SURVIVAL:
                return net.minecraft.world.GameMode.SURVIVAL;
            case ADVENTURE:
                return net.minecraft.world.GameMode.ADVENTURE;
            case SPECTATOR:
                return net.minecraft.world.GameMode.SPECTATOR;
            default:
                throw new IllegalArgumentException("Unknown GameMode: " + gameMode);
        }
    }

    public static GameMode fromFabricGameMode(net.minecraft.world.GameMode fabricGameMode) {
        switch (fabricGameMode) {
            case CREATIVE:
                return GameMode.CREATIVE;
            case SURVIVAL:
                return GameMode.SURVIVAL;
            case ADVENTURE:
                return GameMode.ADVENTURE;
            case SPECTATOR:
                return GameMode.SPECTATOR;
            default:
                throw new IllegalArgumentException("Unknown Fabric GameMode: " + fabricGameMode);
        }
    }
}
