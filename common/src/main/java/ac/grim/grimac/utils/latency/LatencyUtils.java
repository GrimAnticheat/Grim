package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LatencyUtils {
    private final ConcurrentLinkedQueue<QueuedTask> transactionMap = new ConcurrentLinkedQueue<>();
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
        transactionMap.add(new QueuedTask(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        Iterator<QueuedTask> iterator = transactionMap.iterator();
        while (iterator.hasNext()) {
            QueuedTask queuedTask = iterator.next();

            // We are at most a tick ahead when running tasks based on transactions, meaning this is too far
            if (transaction + 1 < queuedTask.transaction)
                break;

            // This is at most tick ahead of what we want
            if (transaction == queuedTask.transaction - 1)
                continue;

            try {
                queuedTask.runnable.run();
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
