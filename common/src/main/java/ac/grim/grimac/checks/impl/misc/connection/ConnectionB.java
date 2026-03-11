package ac.grim.grimac.checks.impl.misc.connection;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.*;

@CheckData(
        name = "ConnectionB",
        description = "Soft ping spoof detection, avoids overflagging"
)
public class ConnectionB extends Check implements PacketCheck {

    private final Deque<Long> pingQueue = new ArrayDeque<>();
    private final Deque<Long> recentDelays = new ArrayDeque<>();

    private long lastPongTime;
    private long lastFlagTime;
    private double buffer;

    // Constants
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final long MAX_ACCEPTABLE_DELAY = NANOS_PER_TICK * 8;
    private static final int MAX_RECENT_PINGS = 20;
    private static final long FLAG_COOLDOWN = 2_000_000_000L;
    private static final double BUFFER_THRESHOLD = 3.0;

    public ConnectionB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PING) return;

        addSample(pingQueue, System.nanoTime());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PONG) return;
        if (pingQueue.isEmpty()) return;

        long now = System.nanoTime();
        long sentTime = pingQueue.poll();

        long delay = Math.abs(now - sentTime);
        lastPongTime = now;

        addSample(recentDelays, delay);

        long medianDelay = calculateMedian(recentDelays);

        updateBuffer(delay, medianDelay);

        if (shouldFlag(now)) {
            handleFlag(now, delay, medianDelay);
        } else {
            reward();
        }
    }

    public void onPredictionComplete(PredictionComplete predictionComplete) {
        long now = System.nanoTime();
        long timeSinceLastPong = now - lastPongTime;

        if (timeSinceLastPong <= NANOS_PER_TICK * 60) return;
        if (now - lastFlagTime <= FLAG_COOLDOWN * 2) return;

        buffer++;

        if (buffer >= BUFFER_THRESHOLD) {
            if (flagAndAlertWithSetback(String.format(
                    "Soft ping spoof | no pong in %.2fms",
                    timeSinceLastPong / 1e6
            ))) {
                lastFlagTime = now;
            }

            buffer = 0;
        }
    }

    private void updateBuffer(long delay, long medianDelay) {
        if (delay > MAX_ACCEPTABLE_DELAY && delay > medianDelay * 2) {
            buffer += 1.0;
        } else {
            buffer = Math.max(0, buffer - 0.5);
        }
    }

    private boolean shouldFlag(long now) {
        return buffer >= BUFFER_THRESHOLD && now - lastFlagTime > FLAG_COOLDOWN;
    }

    private void handleFlag(long now, long delay, long medianDelay) {
        if (flagAndAlert(String.format(
                "Soft ping spoof suspected | delay=%.2fms median=%.2fms buffer=%.1f",
                delay / 1e6,
                medianDelay / 1e6,
                buffer
        ))) {
            lastFlagTime = now;
        }

        buffer = 0;
    }

    private void addSample(Deque<Long> queue, long value) {
        queue.add(value);

        if (queue.size() > MAX_RECENT_PINGS) {
            queue.poll();
        }
    }

    private long calculateMedian(Collection<Long> values) {
        if (values.isEmpty()) return 0L;

        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int mid = sorted.size() / 2;

        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2;
        }

        return sorted.get(mid);
    }
}
