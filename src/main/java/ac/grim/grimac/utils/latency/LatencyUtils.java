package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

import java.util.LinkedList;
import java.util.ListIterator;

public class LatencyUtils {
    private final LinkedList<Pair<Integer, Runnable>> transactionMap = new LinkedList<>();
    private final GrimPlayer player;

    public LatencyUtils(GrimPlayer player) {
        this.player = player;
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        addRealTimeTask(transaction, false, runnable);
    }

    public void addRealTimeTaskAsync(int transaction, Runnable runnable) {
        addRealTimeTask(transaction, true, runnable);
    }

    public void addRealTimeTask(int transaction, boolean async, Runnable runnable) {
        if (player.lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            if (async) {
                ChannelHelper.runInEventLoop(player.user.getChannel(), runnable); // Run it sync to player channel
            } else {
                runnable.run();
            }
            return;
        }
        synchronized (this) {
            transactionMap.add(new Pair<>(transaction, runnable));
        }
    }

    public void handleNettySyncTransaction(int transaction) {
        synchronized (this) {
            for (ListIterator<Pair<Integer, Runnable>> iterator = transactionMap.listIterator(); iterator.hasNext(); ) {
                Pair<Integer, Runnable> pair = iterator.next();

                // We are at most a tick ahead when running tasks based on transactions, meaning this is too far
                if (transaction + 1 < pair.first())
                    return;

                // This is at most tick ahead of what we want
                if (transaction == pair.first() - 1)
                    continue;


                try {
                    // Run the task
                    pair.second().run();
                } catch (Exception e) {
                    System.out.println("An error has occurred when running transactions for player: " + player.user.getName());
                    e.printStackTrace();
                }
                // We ran a task, remove it from the list
                iterator.remove();
            }
        }
    }
}
