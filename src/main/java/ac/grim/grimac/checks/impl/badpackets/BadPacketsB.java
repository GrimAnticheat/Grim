package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(name = "BadPacketsB", description = "Ignored set rotation packet")
public class BadPacketsB extends Check implements PacketCheck {

    public BadPacketsB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTransaction(event.getPacketType())) {
            player.pendingRotations.removeIf(data -> {
                if (player.getLastTransactionReceived() > data.getTransaction()) {
                    if (!data.isAccepted()) {
                        flagAndAlert();
                    }

                    return true;
                }

                return false;
            });
        }
    }
}
