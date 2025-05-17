package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.kyori.adventure.text.Component;
//import net.minecraft.network.RegistryByteBuf;
//import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;

import java.util.function.Function;


public abstract class FabricConversionUtil implements IFabricConversionUtil {

    private IFabricConversionUtil fabricConversionUtilSupplier;

    private Function<net.minecraft.item.ItemStack, ItemStack> itemStackMapperFunction = (fabricStack) -> {
//        if (fabricStack.isEmpty()) {
//            return ItemStack.EMPTY;
//        }
//
//        // Allocate a ByteBuf
//        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
//        try {
//            // Obtain the DynamicRegistryManager (you need to provide this from your context)
//            DynamicRegistryManager registryManager = GrimACFabricLoaderPlugin.FABRIC_SERVER.getRegistryManager(); // Replace with actual method to get registry manager
//
//            // Create a RegistryByteBuf
//            RegistryByteBuf registryByteBuf = new RegistryByteBuf(buffer, registryManager);
//
//            // Encode the ItemStack using the appropriate PacketCodec
//            net.minecraft.item.ItemStack.PACKET_CODEC.encode(registryByteBuf, fabricStack);
//
//            // Create a PacketWrapper to read the ItemStack back (if needed)
//            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer);
//            return wrapper.readItemStack();
//        } catch (Exception e) {
//            // Handle encoding errors
//            LogUtil.error("Failed to encode ItemStack: {}" + fabricStack, e);
//            return ItemStack.EMPTY;
//        } finally {
//            // Release the ByteBuf to prevent memory leaks
//            ByteBufHelper.release(buffer);
//        }
        throw new UnsupportedOperationException();
    };
    private Function<Component, Text> nativeTextMapperFunction = (component) -> {
        throw new UnsupportedOperationException();
//        Text.Serialization.fromJsonTree(GsonComponentSerializer.gson().serializeToTree(component), DynamicRegistryManager.EMPTY);
    };
//

    public ItemStack fromFabricItemStack(net.minecraft.item.ItemStack fabricStack) {
//        return itemStackMapperFunction.apply(fabricStack);
        return fabricConversionUtilSupplier.fromFabricItemStack(fabricStack);
    }

    public Text toNativeText(Component component) {
//        return nativeTextMapperFunction.apply(component);
        return fabricConversionUtilSupplier.toNativeText(component);
    }

    public static net.minecraft.world.GameMode toFabricGameMode(GameMode gameMode) {
        return switch (gameMode) {
            case CREATIVE -> net.minecraft.world.GameMode.CREATIVE;
            case SURVIVAL -> net.minecraft.world.GameMode.SURVIVAL;
            case ADVENTURE -> net.minecraft.world.GameMode.ADVENTURE;
            case SPECTATOR -> net.minecraft.world.GameMode.SPECTATOR;
        };
    }

    public static GameMode fromFabricGameMode(net.minecraft.world.GameMode fabricGameMode) {
        return switch (fabricGameMode) {
            case CREATIVE -> GameMode.CREATIVE;
            case SURVIVAL -> GameMode.SURVIVAL;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
            default -> throw new IllegalArgumentException("Unknown Fabric GameMode: " + fabricGameMode);
        };
    }

    public static InteractionHand fromFabricHand(net.minecraft.util.Hand hand) {
        return hand == null ? null : switch (hand) {
            case OFF_HAND -> InteractionHand.OFF_HAND;
            case MAIN_HAND -> InteractionHand.MAIN_HAND;
        };
    }
}
