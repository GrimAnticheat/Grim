package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.ServerToClientEating;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEating {

    public final ConcurrentLinkedQueue<ServerToClientEating> eatingData = new ConcurrentLinkedQueue<>();
    public final GrimPlayer player;

    public CompensatedEating(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int lastTransactionReceived) {
        while (true) {
            ServerToClientEating data = eatingData.peek();

            if (data == null) break;

            // We don't know if the packet has arrived yet
            if (data.transaction - 1 > lastTransactionReceived) break;
            player.packetStateData.slowedByUsingItem = AlmostBoolean.MAYBE;

            // The packet has 100% arrived
            if (data.transaction > lastTransactionReceived) break;
            eatingData.poll();

            player.packetStateData.slowedByUsingItem = data.isEating ? AlmostBoolean.TRUE : AlmostBoolean.FALSE;

            if (data.isEating) {
                player.packetStateData.eatingHand = data.hand;
            }
        }
    }
}
