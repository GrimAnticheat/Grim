package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;

@CheckData(name = "CrashA")
public class CrashA extends PacketCheck {
    private static final double HARD_CODED_BORDER = 2.9999999E7D;
    private static final double HARD_CODED_ILLEGAL_Y = 1.0E9D;

    public CrashA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation packet = new WrapperPlayClientPlayerPositionAndRotation(event);

            if (Math.abs(packet.getPosition().getX()) > HARD_CODED_BORDER || Math.abs(packet.getPosition().getZ()) > HARD_CODED_BORDER || Math.abs(packet.getPosition().getY()) > HARD_CODED_ILLEGAL_Y) {
                flagAndAlert(); // Ban
            }
        }
    }
}
