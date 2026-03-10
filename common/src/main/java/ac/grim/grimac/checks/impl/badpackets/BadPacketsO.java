package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;

import java.util.LinkedList;

@CheckData(name = "BadPacketsO")
public class BadPacketsO extends Check implements PacketCheck {
    private final LinkedList<Long> keepalives = new LinkedList<>();

    public BadPacketsO(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            keepalives.add(new WrapperPlayServerKeepAlive(event).getId());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            final long id = new WrapperPlayClientKeepAlive(event).getId();

            for (long keepalive : keepalives) {
                if (keepalive == id) {
                    // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
                    Long data;
                    do {
                        data = keepalives.poll();
                    } while (data != null && data != id);

                    return;
                }
            }

            if (flagAndAlert("id=" + id) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
