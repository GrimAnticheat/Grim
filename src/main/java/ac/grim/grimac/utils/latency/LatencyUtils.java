package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class LatencyUtils {
    private final PriorityBlockingQueue<Pair<Integer, Runnable>> transactionMap = new PriorityBlockingQueue<>(64, Comparator.comparingInt(Pair::getFirst));
    private final GrimPlayer player;

    public LatencyUtils(GrimPlayer player) {
        this.player = player;
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        if (player.lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            runnable.run();
            return;
        }
        transactionMap.add(new Pair<>(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        Pair<Integer, Runnable> next = transactionMap.peek();
        while (next != null) {
            // This is a tick ahead of what we want
            if (transaction < next.getFirst())
                break;

            transactionMap.poll();
            next.getSecond().run();
            next = transactionMap.peek();
        }
    }
}
