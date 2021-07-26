package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerCheck extends Check {
    public int exempt = 400; // Exempt for 20 seconds on login
    GrimPlayer player;

    AtomicInteger lastTransactionSent = new AtomicInteger(0);
    AtomicInteger lastTransactionReceived = new AtomicInteger(0);

    int timerTransaction = 0;

    // To patch out lag spikes
    long lastLagSpike = 0;
    int beginningLagSpikeTransaction = 0;
    int transactionsReceivedAtEndOfLastCheck = 0;

    ConcurrentLinkedQueue<Integer> trackedTransactions = new ConcurrentLinkedQueue<>();

    // Proof for this timer check
    // https://i.imgur.com/Hk2Wb6c.png
    //
    // The largest gap will always be the transaction ping (server -> client -> server)
    // Proof lies that client -> server ping will always be lower
    //
    // The largest gap is the floor for movements
    // We increment this by 1 every time we get a movement
    // If the smaller gap surpasses the larger gap, the player is cheating
    //
    // This usually flags 1.01 on low ping extremely quickly
    // Higher ping scales proportionately, and will flag less quickly but still can flag 1.01
    //
    // This is better than traditional timer checks because ping fluctuations will never affect this check
    // As we are tying this check to the player's ping, rather than real time.
    public TimerCheck(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int id) {
        Integer oldestTrackedID = trackedTransactions.peek();
        if (oldestTrackedID != null && id >= oldestTrackedID) {
            trackedTransactions.poll();
            lastTransactionReceived.getAndIncrement();
        }
    }

    public void trackTransaction(int id) {
        lastTransactionSent.getAndIncrement();
        trackedTransactions.add(id);
    }

    public void processMovementPacket() {
        player.movementPackets++;

        // Teleporting sends it's own packet (We could handle this, but it's not worth the complexity)
        if (exempt-- > 0) {
            return;
        }

        if (timerTransaction++ > lastTransactionSent.get()) {
            Bukkit.broadcastMessage(ChatColor.RED + "THE PLAYER HAS TIMER! (Check stable as of 7/25/21, report if not timer!)");
            // Reset the violation by 1 movement
            timerTransaction--;
        }

        /*Bukkit.broadcastMessage("==================");
        Bukkit.broadcastMessage("Sent: " + lastTransactionSent.get());
        Bukkit.broadcastMessage("Timer: " + timerTransaction);
        Bukkit.broadcastMessage("Received: " + lastTransactionReceived.get());
        Bukkit.broadcastMessage("==================");*/

        if (lastTransactionReceived.get() - transactionsReceivedAtEndOfLastCheck > 2) {
            // Stop players from spamming lag spikes to become exempt
            // Spamming F3 + T, I can still flag 1.07 timer
            // Probably can still flag lower over more time, if the client is spamming fake lag spikes
            timerTransaction = Math.max(timerTransaction, beginningLagSpikeTransaction);
            beginningLagSpikeTransaction = transactionsReceivedAtEndOfLastCheck;
            lastLagSpike = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastLagSpike > 1000) {
            timerTransaction = Math.max(timerTransaction, lastTransactionReceived.get());
        }

        transactionsReceivedAtEndOfLastCheck = lastTransactionReceived.get();
    }
}
