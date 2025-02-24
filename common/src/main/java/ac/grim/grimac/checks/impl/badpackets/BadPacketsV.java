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

@CheckData(name = "BadPacketsV", description = "Did not move far enough", experimental = true)
public class BadPacketsV extends Check implements PacketCheck {
    public BadPacketsV(GrimPlayer player) {
        super(player);
    }

    private int noReminderTicks;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.canSkipTicks() && isTickPacket(event.getPacketType())) {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                int positionAtLeastEveryNTicks = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;

                if (noReminderTicks < positionAtLeastEveryNTicks && !player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(1)) {
                    final double deltaSq = new WrapperPlayClientPlayerFlying(event).getLocation().getPosition()
                            .distanceSquared(new Vector3d(player.lastX, player.lastY, player.lastZ));
                    if (deltaSq <= player.getMovementThreshold() * player.getMovementThreshold()) {
                        flagAndAlert("delta=" + Math.sqrt(deltaSq));
                    }
                }

                noReminderTicks = 0;
            } else {
                noReminderTicks++;
            }
        }
    }
}
