package ac.grim.grimac.utils.payload;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import org.jetbrains.annotations.NotNull;

/**
 * Payload wrapper for serverbound {@code MC|ItemName}, replaced by the {@link WrapperPlayClientNameItem NAME_ITEM} packet in 1.13
 */
public record PayloadItemName(@NotNull String itemName) implements Payload {
    public PayloadItemName(byte[] data) {
        this(Payload.wrapper(data).readString());
    }

    @Override
    public void write(PacketWrapper<?> wrapper) {
        wrapper.writeString(itemName);
    }
}
