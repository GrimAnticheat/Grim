package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.ServerBoundPacket;
import org.jetbrains.annotations.NotNull;

public interface PacketListener {
    // please don't mutate this
    @NotNull ServerBoundPacket @NotNull [] TICK_PACKET_CANDIDATES = {
            PacketType.Play.Client.CLIENT_TICK_END,
            PacketType.Play.Client.PLAYER_FLYING,
            PacketType.Play.Client.PLAYER_POSITION,
            PacketType.Play.Client.PLAYER_ROTATION,
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
    };

    // please don't mutate this
    @NotNull ServerBoundPacket @NotNull [] FLYING_PACKETS = {
            PacketType.Play.Client.PLAYER_FLYING,
            PacketType.Play.Client.PLAYER_POSITION,
            PacketType.Play.Client.PLAYER_ROTATION,
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
    };

    // please don't mutate this
    @NotNull ServerBoundPacket @NotNull [] ANIMATION_PACKETS = {
            PacketType.Play.Client.ANIMATION,
            PacketType.Play.Client.PUNCH
    };

    void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry);
}
