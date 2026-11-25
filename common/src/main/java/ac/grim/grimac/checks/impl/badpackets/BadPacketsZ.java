package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "BadPacketsZ", stableKey = "grim.badpackets.duplicate_player_input", description = "Sent duplicate player input packets in the same client tick", experimental = true)
public class BadPacketsZ extends Check implements PreViaPacketReceiveListener {
    private boolean sent;

    public BadPacketsZ(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            sent = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            if (sent) {
                flag();
            }

            sent = true;
        }
    }
}
