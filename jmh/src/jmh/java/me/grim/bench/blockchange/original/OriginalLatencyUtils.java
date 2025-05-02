package me.grim.bench.blockchange.original;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class OriginalLatencyUtils implements ILatencyUtils {
    private final LinkedList<Pair<Integer, Runnable>> transactionMap = new LinkedList<>();
    private final GrimPlayer player;

    // Built from transactionMap and cleared at start of every handleNettySyncTransaction() call
    // The actual usage scope of this variable's use is limited to within the synchronized block of handleNettySyncTransaction
    private final ArrayList<Runnable> tasksToRun = new ArrayList<>();

    public OriginalLatencyUtils(GrimPlayer player) {
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
            tasksToRun.clear();

            // First pass: collect tasks and mark them for removal
            ListIterator<Pair<Integer, Runnable>> iterator = transactionMap.listIterator();
            while (iterator.hasNext()) {
                Pair<Integer, Runnable> pair = iterator.next();

                // We are at most a tick ahead when running tasks based on transactions, meaning this is too far
                if (transaction + 1 < pair.first())
                    break;

                // This is at most tick ahead of what we want
                if (transaction == pair.first() - 1)
                    continue;

                tasksToRun.add(pair.second());
                iterator.remove();
            }

            for (Runnable runnable : tasksToRun) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LogUtil.error("An error has occurred when running transactions for player: " + player.user.getName(), e);
                    // Kick the player SO PEOPLE ACTUALLY REPORT PROBLEMS AND KNOW WHEN THEY HAPPEN
                    if (!Boolean.getBoolean("grim.disable-transaction-kick")) {
                        player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
                    }
                }
            }
        }
    }

    @Override
    public Object getTasksObject() {
        return transactionMap;
    }
}
