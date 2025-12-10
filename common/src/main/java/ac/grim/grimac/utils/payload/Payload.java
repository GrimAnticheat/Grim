package ac.grim.grimac.utils.payload;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

import static com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper.buffer;
import static com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper.wrappedBuffer;
import static com.github.retrooper.packetevents.wrapper.PacketWrapper.createUniversalPacketWrapper;

public interface Payload {
    @NonExtendable
    default byte[] write() {
        Object buffer = buffer();
        write(createUniversalPacketWrapper(buffer));
        return ByteBufHelper.array(buffer);
    }

    @OverrideOnly
    void write(PacketWrapper<?> wrapper);

    static @NotNull PacketWrapper<?> wrapper(byte[] data) {
        return createUniversalPacketWrapper(wrappedBuffer(data));
    }
}
