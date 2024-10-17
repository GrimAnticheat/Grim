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

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            if (!player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                    && !player.packetStateData.lastPacketWasTeleport
                    && player.checkManager.getPacketCheck(BadPacketsE.class).noReminderTicks < 20
                    && !player.skippedTickInActualMovement
                    && !player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(2)
                    && !player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(1)
            ) {
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
    }
}
