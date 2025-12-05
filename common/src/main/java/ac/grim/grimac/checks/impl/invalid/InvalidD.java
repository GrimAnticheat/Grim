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

@CheckData(name = "InvalidD", experimental = true, description = "Soft ping spoof detection, avoids overflagging")
public class InvalidD extends Check implements PostPredictionCheck {

    private final Deque<Long> pingQueue = new LinkedList<>();
    private final Deque<Long> recentDelays = new LinkedList<>();

    private long lastPongTime = 0L;
    private long lastFlagTime = 0L;
    private double buffer = 0;

    // Configurables
    private static final long NANOS_PER_TICK = (long) (50e6); // 50ms per tick
    private static final long MAX_ACCEPTABLE_DELAY = NANOS_PER_TICK * 8; // 8 ticks (~400ms)
    private static final int MAX_RECENT_PINGS = 20;
    private static final long FLAG_COOLDOWN = (long) (2e9); // 2 seconds
    private static final double BUFFER_THRESHOLD = 3.0; // require accumulation

    public InvalidD(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            pingQueue.add(System.nanoTime());
            if (pingQueue.size() > MAX_RECENT_PINGS)
                pingQueue.poll();
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PONG)
            return;
        if (pingQueue.isEmpty())
            return;

        long now = System.nanoTime();
        long sentTime = pingQueue.poll();
        long delay = Math.abs(now - sentTime);
        lastPongTime = now;

        recentDelays.add(delay);
        if (recentDelays.size() > MAX_RECENT_PINGS)
            recentDelays.poll();

        long medianDelay = calculateMedian(recentDelays);

        // Only consider it suspicious if delay is significantly higher than median
        if (delay > MAX_ACCEPTABLE_DELAY && delay > medianDelay * 2) {
            buffer += 1.0; // accumulate suspicious behavior
        } else {
            buffer = Math.max(0, buffer - 0.5); // reduce buffer for normal behavior
        }

        if (buffer >= BUFFER_THRESHOLD && now - lastFlagTime > FLAG_COOLDOWN) {
            if (flagAndAlert(String.format(
                    "Soft ping spoof suspected | delay=%.2fms median=%.2fms buffer=%.1f",
                    delay / 1e6, medianDelay / 1e6, buffer))) {
                lastFlagTime = now;
            }
            buffer = 0;
        } else {
            reward();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        long now = System.nanoTime();
        long timeSinceLastPong = now - lastPongTime;

        if (timeSinceLastPong > NANOS_PER_TICK * 60 && now - lastFlagTime > FLAG_COOLDOWN * 2) {
            buffer += 1.0;
            if (buffer >= BUFFER_THRESHOLD) {
                if (flagAndAlertWithSetback(String.format(
                        "Soft ping spoof | no pong in %.2fms",
                        timeSinceLastPong / 1e6))) {
                    lastFlagTime = now;
                }
                buffer = 0;
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
}
