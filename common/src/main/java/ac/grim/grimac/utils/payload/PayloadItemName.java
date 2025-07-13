package ac.grim.grimac.utils.payload;

import org.jetbrains.annotations.NotNull;

public record PayloadItemName(@NotNull String itemName) {
    public static final PayloadCodec<PayloadItemName> CODEC = new PayloadCodec<>(
            wrapper -> new PayloadItemName(wrapper.readString()),
            (wrapper, payload) -> wrapper.writeString(payload.itemName)
    );
}
