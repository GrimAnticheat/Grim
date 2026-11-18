package ac.grim.grimac.platform.fabric.mc1161.util.convert;

import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.platform.fabric.utils.convert.FabricIntermediaryConversionUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.GameType;

public class Fabric1140ConversionUtil implements IFabricConversionUtil {
    @Override
    public ItemStack fromFabricItemStack(Object fabricItemStack) {
        net.minecraft.world.item.ItemStack fabricStack = (net.minecraft.world.item.ItemStack) fabricItemStack;
        if (fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(buffer);
            packetByteBuf.writeItem(fabricStack);
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
        return net.minecraft.network.chat.Component.Serializer.fromJson(AdventureSerializer.serializer().asJsonTree(component));
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
