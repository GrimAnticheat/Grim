package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TimerCheck extends Check {
    public int exempt = 200; // Exempt for 10 seconds on login
    GrimPlayer player;

    long timerTransaction = 0;

    // To patch out lag spikes
    long lastLagSpike = 0;
    long beginningLagSpikeReceivedRealtime = 0;
    long transactionsReceivedAtEndOfLastCheck = 0;

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

    public void processMovementPacket() {
        player.movementPackets++;

        // Teleporting sends it's own packet (We could handle this, but it's not worth the complexity)
        if (exempt-- > 0) {
            return;
        }

        timerTransaction += 50;

        if (timerTransaction > System.currentTimeMillis()) {
            Bukkit.broadcastMessage(ChatColor.RED + "THE PLAYER HAS TIMER! (Check stable as of 7/25/21, report if not timer!)");
            // Reset the violation by 1 movement
            timerTransaction -= 50;
        }

        /*Bukkit.broadcastMessage("==================");
        Bukkit.broadcastMessage("Timer: " + (System.currentTimeMillis() - timerTransaction));
        Bukkit.broadcastMessage("Received: " + (System.currentTimeMillis() - player.getPlayerClockAtLeast()));
        Bukkit.broadcastMessage("==================");*/

        // Detect lag spikes of minimum 130 ms (missing 2 transactions missing)
        if (System.currentTimeMillis() - transactionsReceivedAtEndOfLastCheck > 130) {
            // Stop players from spamming lag spikes to become exempt
            // Spamming F3 + T, I can still flag 1.07 timer
            // Probably can still flag lower over more time, if the client is spamming fake lag spikes
            timerTransaction = Math.max(timerTransaction, beginningLagSpikeReceivedRealtime);
            beginningLagSpikeReceivedRealtime = transactionsReceivedAtEndOfLastCheck;
            lastLagSpike = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastLagSpike > 1000) {
            timerTransaction = Math.max(timerTransaction, player.getPlayerClockAtLeast());
        }

        transactionsReceivedAtEndOfLastCheck = player.getPlayerClockAtLeast();
    }
}
