package me.grim.bench.blockchange.bit_repack;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class V3LatencyUtils implements ILatencyUtils {
    private final Queue<TransactionTask> transactionMap = new ConcurrentLinkedQueue<>();
    private final GrimPlayer player;

    private record TransactionTask(int transactionId, Runnable task) {}

    public V3LatencyUtils(GrimPlayer player) {
        this.player = player;
    }

    public void addRealTimeTaskAsync(int transaction, Runnable runnable) {
        addRealTimeTask(transaction, true, runnable);
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        addRealTimeTask(transaction, false, runnable);
    }

    public void addRealTimeTask(int transaction, boolean async, Runnable runnable) {
        //if (nettyThread != null && nettyThread != Thread.currentThread()) throw new RuntimeException("Task ran on incorrect thread!");
        if (player.lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            if (async) {
                ChannelHelper.runInEventLoop(player.user.getChannel(), runnable);
            } else {
                runnable.run();
            }
            return;
        }
        transactionMap.add(new TransactionTask(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        for (Iterator<TransactionTask> iterator = transactionMap.iterator(); iterator.hasNext(); ) {
            TransactionTask pair = iterator.next();

            // We are at most a tick ahead when running tasks based on transactions, meaning this is too far
            if (transaction + 1 < pair.transactionId())
                return;

            // This is at most tick ahead of what we want
            if (transaction == pair.transactionId() - 1)
                continue;

            try {
                // Run the task
                pair.task().run();
            } catch (Exception e) {
                LogUtil.error("An error has occurred when running transactions for player: " + player.user.getName(), e);
                // Kick the player SO PEOPLE ACTUALLY REPORT PROBLEMS AND KNOW WHEN THEY HAPPEN
                if (!Boolean.getBoolean("grim.disable-transaction-kick")) {
                    player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
                }
            }
            // We ran a task, remove it from the list
            iterator.remove();
        }
    }
}
