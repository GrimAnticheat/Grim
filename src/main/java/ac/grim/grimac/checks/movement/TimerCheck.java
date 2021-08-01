package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.pair.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TimerCheck extends Check {
    public int exempt = 200; // Exempt for 10 seconds on login
    GrimPlayer player;

    long timerBalanceRealTime = 0;

    // To patch out lag spikes
    long timeSinceLastProcessedMovement = 0;
    // Default value is real time minus max keep-alive time
    long transactionsReceivedAtEndOfLastCheck = (long) (System.nanoTime() - 6e10);

    ConcurrentLinkedQueue<Pair<Long, Long>> lagSpikeToRealTimeFloor = new ConcurrentLinkedQueue<>();

    // Proof for this timer check
    // https://i.imgur.com/Hk2Wb6c.png
    //
    // The largest gap will always be the transaction ping (server -> client -> server)
    // Proof lies that client -> server ping will always be lower
    //
    // The largest gap is the floor for movements
    // If the smaller gap surpasses the larger gap, the player is cheating
    //
    // This usually flags 1.01 on low ping extremely quickly
    // Higher ping/low fps scales proportionately, and will flag less quickly but will still always flag 1.01
    // Players standing still will reset this amount of time
    //
    // This is better than traditional timer checks because ping fluctuations will never affect this check
    // As we are tying this check to the player's ping, rather than real time.
    //
    // Tested 10/20/30 fps and f3 + t spamming for lag spikes at 0 ping localhost/200 ping clumsy, no falses
    public TimerCheck(GrimPlayer player) {
        this.player = player;
    }

    public void processMovementPacket() {
        player.movementPackets++;
        long currentNanos = System.nanoTime();

        // Teleporting sends its own packet (We could handle this, but it's not worth the complexity)
        if (exempt-- > 0) {
            return;
        }

        timerBalanceRealTime += 5e7;

        if (timerBalanceRealTime > currentNanos) {
            Bukkit.broadcastMessage(ChatColor.RED + "THE PLAYER HAS TIMER! (report on discord if not timer)");
            // Reset the violation by 1 movement
            timerBalanceRealTime -= 5e7;
        }

        /*Bukkit.broadcastMessage("==================");
        Bukkit.broadcastMessage("Timer: " + (System.currentTimeMillis() - timerBalanceRealTime));
        Bukkit.broadcastMessage("Received: " + (System.currentTimeMillis() - player.getPlayerClockAtLeast()));
        Bukkit.broadcastMessage("==================");*/

        // Calculate time since last transaction - affected by 50 ms delay movement packets and
        timeSinceLastProcessedMovement = currentNanos + (currentNanos - transactionsReceivedAtEndOfLastCheck);
        // As we don't check players standing still, cap this at 1000 ms
        // A second is more than enough time for all packets from the lag spike to arrive
        // Exempting over a 30 second lag spike will lead to bypasses where the player can catch up movement
        // packets that were lost by standing still
        timeSinceLastProcessedMovement = (long) Math.min(timeSinceLastProcessedMovement, currentNanos + 1e9);

        // Add this into a queue so that new lag spikes do not override previous lag spikes
        lagSpikeToRealTimeFloor.add(new Pair<>(timeSinceLastProcessedMovement, transactionsReceivedAtEndOfLastCheck));

        // Find the safe floor, lag spikes affect transactions, which is bad.
        Pair<Long, Long> lagSpikePair = lagSpikeToRealTimeFloor.peek();
        if (lagSpikePair != null) {
            do {
                if (currentNanos > lagSpikePair.getFirst()) {
                    timerBalanceRealTime = Math.max(timerBalanceRealTime, lagSpikePair.getSecond());

                    lagSpikeToRealTimeFloor.poll();
                    lagSpikePair = lagSpikeToRealTimeFloor.peek();
                } else {
                    break;
                }
            } while (lagSpikePair != null);
        }

        transactionsReceivedAtEndOfLastCheck = player.getPlayerClockAtLeast();
    }
}
