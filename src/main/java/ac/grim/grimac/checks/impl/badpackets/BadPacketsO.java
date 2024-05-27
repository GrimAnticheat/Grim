package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;

import java.util.LinkedList;
import java.util.Queue;

@CheckData(name = "BadPacketsO")
public class BadPacketsO extends Check implements PacketCheck {
    private final Queue<Pair<Long, Long>> keepaliveMap = new LinkedList<>();

    public BadPacketsO(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.KEEP_ALIVE) return;

        WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
        keepaliveMap.add(new Pair<>(packet.getId(), System.nanoTime()));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) return;

        final WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

        final long id = packet.getId();
        final boolean hasID = keepaliveMap.stream().anyMatch(pair -> pair.getFirst().equals(id));

        if (!hasID) {
            flagAndAlert("ID: " + id);
            return;
        }
        // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
        Pair<Long, Long> data;
        do {
            data = keepaliveMap.poll();
            if (data == null) break;
        } while (data.getFirst() != id);
    }
}
