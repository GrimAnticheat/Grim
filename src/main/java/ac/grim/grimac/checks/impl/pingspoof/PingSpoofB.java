package ac.grim.grimac.checks.impl.pingspoof;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PingSpoofB extends PacketCheck {
    Queue<Pair<Long, Long>> keepaliveMap = new ConcurrentLinkedQueue<>();

    public PingSpoofB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
            keepaliveMap.add(new Pair<>(packet.getId(), System.nanoTime()));
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

            long id = packet.getId();
            boolean hasID = false;

            for (Pair<Long, Long> iterator : keepaliveMap) {
                if (iterator.getFirst() == id) {
                    hasID = true;
                    break;
                }
            }

            long ping = 0;

            if (hasID) {
                Pair<Long, Long> data;
                do {
                    data = keepaliveMap.poll();

                    if (data == null)
                        break;

                    ping = (int) (System.nanoTime() - data.getSecond());
                } while (data.getFirst() != id);
            }

            double ms = (player.getTransactionPing() - ping) / 1e6;

            // TODO: Refine ping spoofing checks
            if (ms > 120) {
                //flag();
            } else {
                //reward();
            }
        }
    }
}
