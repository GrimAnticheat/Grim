package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.Arrays;
import java.util.List;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "BadPacketsQ", experimental = true)
public class BadPacketsQ extends PacketCheck {

    public BadPacketsQ(GrimPlayer playerData) {
        super(playerData);
    }

    private long lastMovementPacket = -1;

    private static final List<PacketTypeCommon> packetTypes = Arrays.asList(
            PacketType.Play.Client.ENTITY_ACTION,
            PacketType.Play.Client.PONG,
            PacketType.Play.Client.ANIMATION,
            PacketType.Play.Client.PLAYER_DIGGING);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) { // TODO: Use grim's lag compensated alien technology to make laggers not false this
        if (isFlying(event.getPacketType())) {
            lastMovementPacket = System.currentTimeMillis();
        } else if (packetTypes.contains(event.getPacketType())) {
            long timeSinceLastMove = System.currentTimeMillis() - lastMovementPacket;
            if (lastMovementPacket != -1 && timeSinceLastMove > 2000) {
                flagAndAlert("timeSince=" + timeSinceLastMove + " packet=" + event.getPacketType().getName());
            }
        }
    }
}
