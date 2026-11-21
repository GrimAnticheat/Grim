package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

/** Native conversion shares the wire encoder so typed hover and click payloads remain typed. */
public final class FabricComponentDecoder {
    private FabricComponentDecoder() {
    }

    public static <T> T decode(Component message, Function<ByteBuf, T> decoder) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            PacketWrapper.createUniversalPacketWrapper(buffer).writeComponent(message);
            return decoder.apply(buffer);
        } finally {
            buffer.release();
        }
    }
}
