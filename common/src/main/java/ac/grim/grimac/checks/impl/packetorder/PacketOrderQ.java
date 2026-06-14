package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(name = "PacketOrderQ", description = "Has position changed without transactions packet", experimental = true)
public class PacketOrderQ extends Check implements PacketCheck {
    private int positions = 0;
    private long lastTransTime = 0L;

    public PacketOrderQ(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = event.getTimestamp();

        // If a valid transaction has arrived, we reset the counter.
        if (isTransaction(type) && player.packetStateData.lastTransactionPacketWasValid) {
            positions = 0;
            lastTransTime = now;
            return;
        }

        // Count the pos packets
        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || type == PacketType.Play.Client.PLAYER_POSITION) {
            positions++;

            // If 40 position packets have already arrived (~2 seconds), but there are zero transactions
            if (positions > 40 && now - player.joinTime > 5000L) {
                flagAndAlert("positions=" + positions + ", last=" + (now - lastTransTime) + "ms");
                player.getSetbackTeleportUtil().executeViolationSetback();

                positions = 0;
            }
        }
    }
}
