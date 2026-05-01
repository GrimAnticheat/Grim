package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "PacketOrderP", stableKey = "grim.packetorder.transaction_keepalive_order", description = "Sent keepalive and transaction packets out of order", experimental = true)
public class PacketOrderP extends Check implements PacketCheck {

    private final List<KeepAliveData> pendingKeepAlives = new ArrayList<>();

    public PacketOrderP(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            long id = new WrapperPlayClientKeepAlive(event).getId();
            pendingKeepAlives.removeIf(p -> p.id() == id);
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);

            // if the server sends the same keepalive id twice and the client is lagging, we can get this edge case:
            // keepalive(S){id:1} -> transaction(S){id:1} -> keepalive(C){id:1} -> keepalive(S){id:1} -> transaction(S){id:2} -> transaction(C){id:1}
            // and at that point keepalive{id:1} would not be accepted, so the check would false.
            // using an object instead of just the id prevents this false because the first & second objects would be different.
            KeepAliveData data = new KeepAliveData(packet.getId());

            pendingKeepAlives.add(data);

            player.addRealTimeTaskNext(() -> {
                if (pendingKeepAlives.contains(data)) {
                    flagAndAlert();
                }
            });

            event.getTasksAfterSend().add(player::sendTransaction);
        }
    }

    private record KeepAliveData(long id) {}
}
