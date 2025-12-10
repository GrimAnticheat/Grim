package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.collections.FastIteLinkedQueue;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LatencyUtils {
    private final Queue<QueuedTask> transactions = new FastIteLinkedQueue<>();
    private final Queue<QueuedTask> transactionsAsync = new ConcurrentLinkedQueue<>();
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
                player.runSafely(runnable);
            } else {
                runnable.run();
            }
            return;
        }
        queueTask(transaction, runnable, async);
    }

    private void queueTask(int transaction, Runnable runnable, boolean async) {
        Queue<QueuedTask> queue = async ? transactionsAsync : transactions;
        queue.add(new QueuedTask(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        handleQueue(transaction, true, transactions);
        // ConcurrentLinkedQueue is weakly consistent, we need to iterate over all elements
        // to make sure we don't miss anything.
        handleQueue(transaction, false, transactionsAsync);
    }

    private void handleQueue(int transaction, boolean breakOnAhead, Queue<QueuedTask> queue) {
        Iterator<QueuedTask> iterator = queue.iterator();
        while (iterator.hasNext()) {
            QueuedTask task = iterator.next();

            // Tick ahead of
            if (transaction < task.transaction) {
                if (breakOnAhead && transaction + 1 < task.transaction)
                    break;
                else
                    continue;
            }

            try {
                task.runnable.run();
            } catch (Exception e) {
                LogUtil.error("An error has occurred when running transactions for player: " + player.user.getName(), e);
                // Kick the player SO PEOPLE ACTUALLY REPORT PROBLEMS AND KNOW WHEN THEY HAPPEN
                if (CommonGrimArguments.KICK_ON_TRANSACTION_ERRORS.value()) {
                    player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
                }
            }
            iterator.remove();
        }
    }

    private record QueuedTask(int transaction, Runnable runnable) {}

}
