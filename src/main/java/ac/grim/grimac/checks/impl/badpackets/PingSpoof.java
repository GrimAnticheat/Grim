package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.*;

@CheckData(name = "PingSpoof", description = "Detects ping spoof/fake lag/blink")
public class PingSpoof extends Check implements PostPredictionCheck {

    private final Deque<Long> ping = new LinkedList<>();
    private final Deque<Long> recentPings = new LinkedList<>();

    private long lastPingSentTime = 0L;
    private long lastPongTime = 0L;
    private long lastFlagTime = 0L;

    private static final long NANOS_PER_TICK = (long)(50e6);
    private static final long MAX_ACCEPTABLE_DELAY = NANOS_PER_TICK * 3;
    private static final int MAX_RECENT_PINGS = 20;
    private static final long FLAG_COOLDOWN = (long)(500e6);

    public PingSpoof(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            long now = System.nanoTime();

            lastPingSentTime = now;
            ping.add(now);

            if (ping.size() > MAX_RECENT_PINGS) {
                ping.poll();
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            long now = System.nanoTime();

            long sentTime = ping.isEmpty() ? lastPingSentTime : ping.poll();
            long delay = now - sentTime;

            lastPongTime = now;

            // Add current delay to recent ping list
            recentPings.add(delay);
            if (recentPings.size() > MAX_RECENT_PINGS) {
                recentPings.poll();
            }

            // Calculate smoothed average
            long averageDelay = recentPings.stream()
                    .mapToLong(Long::longValue)
                    .sum() / recentPings.size();

            // Check if the delay is suspiciously high
            if (delay > MAX_ACCEPTABLE_DELAY && delay > averageDelay * 1.5) {
                if (now - lastFlagTime > FLAG_COOLDOWN) {
                    flagAndAlert("Abnormal pong delay: " + (delay / 1e6) + "ms (Avg: " + (averageDelay / 1e6) + "ms)");
                    lastFlagTime = now;
                }
            } else {
                reward();
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        long now = System.nanoTime();
        long timeSinceLastPong = now - lastPongTime;

        // If the server hasn't received any pong for a while, it's suspicious
        if (timeSinceLastPong > NANOS_PER_TICK * 40) { // ~2 seconds
            if (now - lastFlagTime > FLAG_COOLDOWN * 2) {
                flagAndAlertWithSetback("No pong in " + (timeSinceLastPong / 1e6) + "ms — possible blink/fake lag");
                lastFlagTime = now;
            }
        }
    }
}
