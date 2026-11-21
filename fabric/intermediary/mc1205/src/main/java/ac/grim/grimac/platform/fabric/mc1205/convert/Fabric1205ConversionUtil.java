package ac.grim.grimac.platform.fabric.mc1205.convert;

import ac.grim.grimac.platform.fabric.GrimACFabricIntermediaryLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.FabricComponentDecoder;
import ac.grim.grimac.platform.fabric.utils.convert.FabricIntermediaryConversionUtil;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.GameType;

public class Fabric1205ConversionUtil implements IFabricConversionUtil {
    @Override
    public ItemStack fromFabricItemStack(Object fabricItemStack) {
        net.minecraft.world.item.ItemStack fabricStack = (net.minecraft.world.item.ItemStack) fabricItemStack;
        if (fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            RegistryAccess registryManager = GrimACFabricIntermediaryLoaderPlugin.FABRIC_SERVER.registryAccess();

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
        return FabricComponentDecoder.decode(component, buffer ->
                ComponentSerialization.TRUSTED_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buffer,
                        GrimACFabricIntermediaryLoaderPlugin.FABRIC_SERVER.registryAccess())));
    }

    @Override
    public GameMode fromNativeGameMode(Object gameMode) {
        return FabricIntermediaryConversionUtil.fromFabricGameMode((GameType) gameMode);
    }

    @Override
    public Object toNativeGameMode(GameMode gameMode) {
        return FabricIntermediaryConversionUtil.toFabricGameMode(gameMode);
    }

    @Override
    public InteractionHand fromFabricInteractionHand(Object hand) {
        return FabricIntermediaryConversionUtil.fromFabricInteractionHand((net.minecraft.world.InteractionHand) hand);
    }
}
