package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.pair.Pair;

import java.util.concurrent.ConcurrentLinkedQueue;

@CheckData(name = "Timer (Experimental)", configName = "TimerA", flagCooldown = 1000, maxBuffer = 5)
public class TimerCheck extends PacketCheck {
    public int exempt = 200; // Exempt for 10 seconds on login
    GrimPlayer player;

    long timerBalanceRealTime = 0;

    // Default value is real time minus max keep-alive time
    long knownPlayerClockTime = (long) (System.nanoTime() - 6e10);

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
        super(player);
        this.player = player;
    }

    public void onPacketReceive(final PacketPlayReceiveEvent event) {
        long currentNanos = System.nanoTime();

        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        if (!PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId()) ||
                player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            return;
        }
        player.movementPackets++;
        knownPlayerClockTime = player.getPlayerClockAtLeast();

        // Teleporting sends its own packet (We could handle this, but it's not worth the complexity)
        if (exempt-- > 0) {
            return;
        }
        exempt = 0;

        timerBalanceRealTime += 50e6;

        if (timerBalanceRealTime > currentNanos) {
            increaseViolations();
            alert("", "Timer (experimental)", formatViolations());

            // Reset the violation by 1 movement
            timerBalanceRealTime -= 50e6;
        } else {
            // Decrease buffer as to target 1.005 timer - 0.005
            reward();
        }

        // Calculate time since last transaction - affected by 50 ms delay movement packets and
        long timeSinceLastProcessedMovement = currentNanos + (currentNanos - knownPlayerClockTime);

        // Add this into a queue so that new lag spikes do not override previous lag spikes
        lagSpikeToRealTimeFloor.add(new Pair<>(timeSinceLastProcessedMovement, knownPlayerClockTime));

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
    }
}
