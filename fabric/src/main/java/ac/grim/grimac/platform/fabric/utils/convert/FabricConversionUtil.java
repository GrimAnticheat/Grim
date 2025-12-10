package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.kyori.adventure.text.Component;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class FabricConversionUtil implements IFabricConversionUtil {

    private IFabricConversionUtil fabricConversionUtilSupplier;

    private final Function<net.minecraft.world.item.ItemStack, ItemStack> itemStackMapperFunction = (fabricStack) -> {
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
    private final Function<Component, net.minecraft.network.chat.Component> nativeTextMapperFunction = (component) -> {
        throw new UnsupportedOperationException();
//        Text.Serialization.fromJsonTree(GsonComponentSerializer.gson().serializeToTree(component), DynamicRegistryManager.EMPTY);
    };
//

    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
//        return itemStackMapperFunction.apply(fabricStack);
        return fabricConversionUtilSupplier.fromFabricItemStack(fabricStack);
    }

    public net.minecraft.network.chat.Component toNativeText(Component component) {
//        return nativeTextMapperFunction.apply(component);
        return fabricConversionUtilSupplier.toNativeText(component);
    }

    public static GameType toFabricGameMode(GameMode gameMode) {
        return switch (gameMode) {
            case CREATIVE -> GameType.CREATIVE;
            case SURVIVAL -> GameType.SURVIVAL;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }

    public static GameMode fromFabricGameMode(GameType fabricGameMode) {
        return switch (fabricGameMode) {
            case CREATIVE -> GameMode.CREATIVE;
            case SURVIVAL -> GameMode.SURVIVAL;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
            default -> throw new IllegalArgumentException("Unknown Fabric GameMode: " + fabricGameMode);
        };
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static @Nullable InteractionHand fromFabricHand(@Nullable net.minecraft.world.InteractionHand hand) {
        return hand == null ? null : switch (hand) {
            case OFF_HAND -> InteractionHand.OFF_HAND;
            case MAIN_HAND -> InteractionHand.MAIN_HAND;
        };
    }
}
