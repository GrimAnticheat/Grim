package ac.grim.grimac.platform.fabric.mc12110;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record GrimACFabric12110MessagePayload(ResourceLocation key, byte[] data) implements CustomPacketPayload {

    private static final int MAX_PAYLOAD_SIZE = 1048576;

    public static StreamCodec<FriendlyByteBuf, GrimACFabric12110MessagePayload> codec(ResourceLocation key) {
        return CustomPacketPayload.codec(
                (payload, buffer) -> {
                    // write
                    buffer.writeBytes(payload.data);
                },
                (buffer) -> {
                    // read
                    int readableBytes = buffer.readableBytes();
                    if (readableBytes >= 0 && readableBytes <= MAX_PAYLOAD_SIZE) {
                        byte[] data = new byte[readableBytes];
                        buffer.readBytes(data);

                        return new GrimACFabric12110MessagePayload(key, data);
                    }
                    return new GrimACFabric12110MessagePayload(key, new byte[0]);
                });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(this.key);
    }
}
