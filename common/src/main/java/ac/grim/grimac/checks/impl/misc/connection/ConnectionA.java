package ac.grim.grimac.checks.impl.misc.connection;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(
        name = "ConnectionA",
        description = "Detects suspicious ping changes"
)
public class ConnectionA extends Check implements PacketCheck {

    private static final int SAMPLE_SIZE = 35;
    private static final long JOIN_GRACE_PERIOD = 5000L;
    private static final double MAX_AVG_PING = 300.0;
    private static final double BUFFER_THRESHOLD = 4.0;
    private static final double BUFFER_DECAY = 0.5;

    private final EvictingQueue<Double> delays = new EvictingQueue<>(SAMPLE_SIZE);

    private double buffer;

    public ConnectionA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isTickPacket(event.getPacketType())) return;
        if (shouldIgnore()) return;

        double delay = player.getTransactionPing();
        delays.add(delay);

        if (!delays.isCollected()) return;

        handleCollectedSamples(delay);
    }

    private boolean shouldIgnore() {
        return player.packetStateData.lastPacketWasTeleport
                || System.currentTimeMillis() - player.joinTime < JOIN_GRACE_PERIOD
                || player.inVehicle();
    }

    private void handleCollectedSamples(double latestDelay) {
        double averageDelay = GrimMath.getAverage(delays);
        delays.clear();

        if (averageDelay > MAX_AVG_PING) {
            increaseBuffer(averageDelay, latestDelay);
        } else {
            decayBuffer();
        }
    }

    private void increaseBuffer(double avgDelay, double latestDelay) {
        buffer++;

        if (buffer > BUFFER_THRESHOLD) {
            flagAndAlert(String.format(
                    "avg=%.1f, delay=%.1f, buffer=%.1f",
                    avgDelay,
                    latestDelay,
                    buffer
            ));

            if (shouldSetback()) {
                player.getSetbackTeleportUtil().executeViolationSetback();
            }
        }
    }

    private void decayBuffer() {
        if (buffer > 0) {
            buffer = Math.max(0, buffer - BUFFER_DECAY);
        }
    }
}
