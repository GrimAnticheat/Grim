package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class BlockPlaceSnapshot {
    private final PacketWrapper<?> wrapper;
    private final boolean sneaking;
}
