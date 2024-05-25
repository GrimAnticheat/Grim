package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPackets1", experimental = true)
public class BadPackets1 extends Check implements PacketCheck {
    public BadPackets1(GrimPlayer player) {
        super(player);
    }

    private boolean lastLastPacketWasTeleport = true;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            final double delta = Math.sqrt((player.x - player.lastX) * (player.x - player.lastX) + (player.y - player.lastY) * (player.y - player.lastY) + (player.z - player.lastZ) * (player.z - player.lastZ));
            // This seems to be valid for 1.17 duplicates, don't exempt
            if (!player.packetStateData.lastPacketWasTeleport && !lastLastPacketWasTeleport && player.checkManager.getPacketCheck(BadPacketsE.class).noReminderTicks < 20 && !player.skippedTickInActualMovement) {
                if (delta <= player.getMovementThreshold() && flagWithSetback()) alert("delta=" + delta);
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            lastLastPacketWasTeleport = player.packetStateData.lastPacketWasTeleport;
        }
    }
}
