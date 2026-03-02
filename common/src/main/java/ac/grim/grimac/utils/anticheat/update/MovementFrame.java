package ac.grim.grimac.utils.anticheat.update;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.Nullable;

public record MovementFrame(PacketTypeCommon source,
                            boolean hasPosition,
                            boolean hasLook,
                            @Nullable PositionUpdate positionUpdate,
                            @Nullable RotationUpdate rotationUpdate,
                            PacketReceiveEvent event) {
}
