package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import ac.grim.grimac.utils.data.IntToObjectPair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class LatencyUtils {
    private final LinkedList<IntToObjectPair<Runnable>> transactionMap = new LinkedList<>();
    private final GrimPlayer player;

    // Built from transactionMap and cleared at start of every handleNettySyncTransaction() call.
    // Both collections are owned by the player's Netty event loop.
    private final ArrayList<Runnable> tasksToRun = new ArrayList<>();

    public LatencyUtils(GrimPlayer player) {
        this.player = player;
    }

    // The caller owns the player's Netty event loop. Off-thread callers must
    // use addRealTimeTaskAsync so this hot path stays lock-free.
    public void addRealTimeTask(int transaction, Runnable runnable) {
        if (player.lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            runnable.run();
            return;
        }

        transactionMap.add(new IntToObjectPair<>(transaction, runnable));
    }

    /**
     * Schedules task insertion on the player's Netty event loop.
     *
     * <p>The transaction queue is owned by that event loop. This entry point is
     * for callers that already know they are outside it.</p>
     */
    public void addRealTimeTaskAsync(int transaction, Runnable runnable) {
        player.runSafely(() -> addRealTimeTask(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        tasksToRun.clear();

        // First pass: collect and remove tasks before running them. A task may
        // enqueue another task, so executing while iterating would invalidate
        // the iterator even though everything stays on one event-loop thread.
        ListIterator<IntToObjectPair<Runnable>> iterator = transactionMap.listIterator();
        while (iterator.hasNext()) {
            IntToObjectPair<Runnable> pair = iterator.next();

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
                if (CommonGrimArguments.KICK_ON_TRANSACTION_ERRORS.value()) {
                    player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
                }
            }
        }
    }
}
