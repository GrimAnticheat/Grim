package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPackets1", experimental = true)
public class BadPackets1 extends Check implements PacketCheck {
    public BadPackets1(GrimPlayer player) {
        super(player);
    }

    private boolean lastLastPacketWasTeleport = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_4);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            if (!player.packetStateData.lastPacketWasOnePointSeventeenDuplicate && !player.packetStateData.lastPacketWasTeleport && !lastLastPacketWasTeleport && player.checkManager.getPacketCheck(BadPacketsE.class).noReminderTicks < 20 && !player.skippedTickInActualMovement) {
                final double delta = new WrapperPlayClientPlayerFlying(event).getLocation().getPosition().distance(new Vector3d(player.lastX, player.lastY, player.lastZ));
                if (delta <= player.getMovementThreshold() && flagWithSetback()) {
                    alert("delta=" + delta);
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // for lunar & some forge clients. Stopped between 1.13 and 1.16.5, exempt pre-1.16.5 just to be safe
            lastLastPacketWasTeleport = player.packetStateData.lastPacketWasTeleport && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_4);
        }
    }
}
