package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
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
public class BadPacketsO extends AbstractPacketCheck {
    Queue<Pair<Long, Long>> keepaliveMap = new LinkedList<>();

    public BadPacketsO(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerSendHandlers(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
            keepaliveMap.add(new Pair<>(packet.getId(), System.nanoTime()));
        }, PacketType.Play.Server.KEEP_ALIVE);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

            long id = packet.getId();
            boolean hasID = false;

            for (Pair<Long, Long> iterator : keepaliveMap) {
                if (iterator.first() == id) {
                    hasID = true;
                    break;
                }
            }

            if (!hasID) {
                if (flagAndAlert("id=" + id) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else { // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
                Pair<Long, Long> data;
                do {
                    data = keepaliveMap.poll();
                    if (data == null) break;
                } while (data.first() != id);
            }
        }, PacketType.Play.Client.KEEP_ALIVE);
    }
}
