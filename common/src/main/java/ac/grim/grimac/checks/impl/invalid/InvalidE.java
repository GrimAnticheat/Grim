package ac.grim.grimac.checks.impl.invalid;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.*;

@CheckData(name = "InvalidE", check = "Invalid", type = "E", experimental = true, description = "Aggressive detection of ping spoof, fake lag and blink abuse")
public class InvalidE extends Check implements PostPredictionCheck {

    private final Deque<Long> pingQueue = new LinkedList<>();
    private final Deque<Long> recentDelays = new LinkedList<>();

    private long lastPongTime = 0L;
    private long lastFlagTime = 0L;

    // Configuration
    private static final long NANOS_PER_TICK = (long) (50e6); // 50ms
    private static final long MAX_ACCEPTABLE_DELAY = NANOS_PER_TICK * 3; // 3 ticks (~150ms)
    private static final int MAX_RECENT_PINGS = 25;
    private static final long FLAG_COOLDOWN = (long) (500e6); // 0.5 seconds

    public InvalidE(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            pingQueue.add(System.nanoTime());
            if (pingQueue.size() > MAX_RECENT_PINGS) {
                pingQueue.poll();
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            long now = System.nanoTime();
            if (pingQueue.isEmpty())
                return;

            long sentTime = pingQueue.poll();
            long delay = Math.abs(now - sentTime);
            lastPongTime = now;

            recentDelays.add(delay);
            if (recentDelays.size() > MAX_RECENT_PINGS) {
                recentDelays.poll();
            }

            long median = calculateMedian(recentDelays);
            double stdDev = calculateStdDev(recentDelays, median);

            // Aggressive detection: high delay or large inconsistency
            boolean abnormalDelay = delay > MAX_ACCEPTABLE_DELAY && delay > median * 1.4;
            boolean inconsistent = stdDev > median * 0.8; // large variation between responses

            if ((abnormalDelay || inconsistent) && now - lastFlagTime > FLAG_COOLDOWN) {
                flagAndAlert(String.format(
                        "Inconsistent ping detected: delay=%.2fms, median=%.2fms, σ=%.2fms",
                        delay / 1e6, median / 1e6, stdDev / 1e6));
                lastFlagTime = now;
            } else if (!abnormalDelay) {
                reward();
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        long now = System.nanoTime();
        long timeSinceLastPong = now - lastPongTime;

        // Aggressive detection: high delay or large inconsistency
        if (timeSinceLastPong > NANOS_PER_TICK * 25) { // ~1.25 s
            if (now - lastFlagTime > FLAG_COOLDOWN * 2) {
                flagAndAlertWithSetback(String.format(
                        "No pong in %.2fms — possible blink/fake lag",
                        timeSinceLastPong / 1e6));
                lastFlagTime = now;
            }
        }
    }

    private long calculateMedian(Collection<Long> delays) {
        if (delays.isEmpty())
            return 0L;
        List<Long> sorted = new ArrayList<>(delays);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        return (sorted.size() % 2 == 0)
                ? (sorted.get(middle - 1) + sorted.get(middle)) / 2
                : sorted.get(middle);
    }

    private double calculateStdDev(Collection<Long> delays, long median) {
        if (delays.size() < 2)
            return 0.0;
        double mean = delays.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        double variance = 0.0;
        for (long d : delays) {
            variance += Math.pow(d - mean, 2);
        }
        return Math.sqrt(variance / delays.size());
    }
}
