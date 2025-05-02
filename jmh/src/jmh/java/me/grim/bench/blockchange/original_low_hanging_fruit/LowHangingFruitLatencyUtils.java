package me.grim.bench.blockchange.original_low_hanging_fruit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

import java.util.ArrayDeque; // Use ArrayDeque
import java.util.ArrayList;
import java.util.Iterator; // Use standard Iterator

public class LowHangingFruitLatencyUtils implements ILatencyUtils {

    // Record to replace Pair with primitive int
    private record TransactionTask(int transactionId, Runnable task) {}

    private final ArrayDeque<TransactionTask> transactionMap = new ArrayDeque<>();

    private final GrimPlayer player;
    private final ArrayList<Runnable> tasksToRun = new ArrayList<>();

    public LowHangingFruitLatencyUtils(GrimPlayer player) {
        this.player = player;
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        addRealTimeTaskInternal(transaction, false, runnable);
    }

    public void addRealTimeTaskAsync(int transaction, Runnable runnable) {
        addRealTimeTaskInternal(transaction, true, runnable);
    }

    private void addRealTimeTaskInternal(int transactionId, boolean async, Runnable runnable) {
        if (player.lastTransactionReceived.get() >= transactionId) {
            if (async) {
                try {
                    ChannelHelper.runInEventLoop(player.user.getChannel(), runnable);
                } catch (Exception e) {
                    System.err.println("WARN: ChannelHelper execution failed in benchmark: " + e.getMessage());
                    runnable.run();
                }
            } else {
                runnable.run();
            }
            return;
        }
        synchronized (transactionMap) {
            transactionMap.add(new TransactionTask(transactionId, runnable));
        }
    }

    @Override
    public void handleNettySyncTransaction(int receivedTransactionId) {
        synchronized (transactionMap) {
            tasksToRun.clear();

            Iterator<TransactionTask> iterator = transactionMap.iterator();
            while (iterator.hasNext()) {
                TransactionTask taskEntry = iterator.next();
                int taskTransactionId = taskEntry.transactionId();

                // *** Restore the break condition ***
                // If tasks are added with monotonically increasing IDs,
                // once we find one that's too far ahead, all subsequent ones
                // will also be too far ahead. This optimization is valid.
                if (receivedTransactionId + 1 < taskTransactionId) {
                    break; // Optimization restored
                }

                // This is at most tick ahead of what we want
                if (receivedTransactionId == taskTransactionId - 1) {
                    continue; // Skip this specific task
                }

                // If we didn't break or continue, the task is eligible
                tasksToRun.add(taskEntry.task());
                iterator.remove(); // Remove using the iterator
            }

            // Task execution loop
            for (Runnable runnable : tasksToRun) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    handleRunnableError(e);
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized (transactionMap) {
            transactionMap.clear();
        }
        tasksToRun.clear();
    }

    private void handleRunnableError(Exception e) {
        LogUtil.error("An error has occurred when running transactions for player: " + player.user.getName(), e);
        // Kick the player SO PEOPLE ACTUALLY REPORT PROBLEMS AND KNOW WHEN THEY HAPPEN
        if (!Boolean.getBoolean("grim.disable-transaction-kick")) {
            player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
        }
    }

    @Override
    public Object getTasksObject() {
        return transactionMap;
    }
}