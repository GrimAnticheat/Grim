package ac.grim.grimac.platform.fabric.mc12110;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record GrimACFabric12110MessagePayload(ResourceLocation key, ByteBuf data) implements CustomPacketPayload {

    public static StreamCodec<FriendlyByteBuf, GrimACFabric12110MessagePayload> CODEC =
            CustomPacketPayload.codec((discardedPayload, friendlyByteBuf) ->
                // write
                friendlyByteBuf.writeBytes(discardedPayload.data),
            (friendlyByteBuf) -> {
                // read
                friendlyByteBuf.readerIndex(friendlyByteBuf.writerIndex());
                throw new UnsupportedOperationException();
            });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(this.key);
    }
}
