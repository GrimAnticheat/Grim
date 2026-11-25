package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.checks.type.PreViaPacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@CheckData(name = "PacketOrderP", stableKey = "grim.packetorder.transaction_keepalive_order", description = "Sent keepalive and transaction packets out of order", experimental = true)
public class PacketOrderP extends Check implements PreViaPacketSendListener, PreViaPacketReceiveListener {

    private static final int DEFAULT_LIST_SIZE = 4;
    private static final int TARGET_MAX_LIST_SIZE = 8;

    private final ArrayList<KeepAliveData> pendingKeepAlives = new ArrayList<>(DEFAULT_LIST_SIZE);
    private boolean needsShrink;

    public PacketOrderP(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) return;
        long id = new WrapperPlayClientKeepAlive(event).getId();

        for (int i = 0, size = pendingKeepAlives.size(); i < size; i++) {
            if (pendingKeepAlives.get(i).id == id) {
                pendingKeepAlives.remove(i);
                break;
            }
        }

        if (needsShrink && pendingKeepAlives.isEmpty()) {
            pendingKeepAlives.trimToSize(); // sets internal array to a constant if empty
            pendingKeepAlives.ensureCapacity(DEFAULT_LIST_SIZE); // one array allocation
            needsShrink = false;
        }
    }

    @Override
    public void onPreViaPacketSend(final @NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.KEEP_ALIVE) return;
        WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);

        // if the server sends the same keepalive id twice and the client is lagging, we can get this edge case:
        // keepalive(S){id:1} -> transaction(S){id:1} -> keepalive(C){id:1} -> keepalive(S){id:1} -> transaction(S){id:2} -> transaction(C){id:1}
        // and at that point keepalive{id:1} would not be accepted, so the check would false.
        // using an object instead of just the id prevents this false because the first & second objects would be different.
        // This object does not implement equals because if it did, the same problem would occur.
        KeepAliveData data = new KeepAliveData(packet.getId());

        pendingKeepAlives.add(data);

        player.addRealTimeTaskNext(() -> {
            // remove to prevent memory leaks if the client doesn't respond to keepalives for some reason
            if (pendingKeepAlives.remove(data)) {
                flag();
            }
        });

        if (pendingKeepAlives.size() > TARGET_MAX_LIST_SIZE) {
            needsShrink = true;
        }

        event.getTasksAfterSend().add(player::sendTransaction);
    }

    // Do not implement equals! Reference equality is required to distinguish duplicate IDs.
    @SuppressWarnings("ClassCanBeRecord")
    private static final class KeepAliveData {
        public final long id;

        @Contract(pure = true)
        public KeepAliveData(long id) {
            this.id = id;
        }
    }
}
