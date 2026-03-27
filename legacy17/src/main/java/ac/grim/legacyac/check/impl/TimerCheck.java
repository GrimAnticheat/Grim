package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Locale;
import org.bukkit.entity.Player;

/**
 * Timer check backed by real packet timing, with idle heartbeat filtering.
 */
public final class TimerCheck extends Check {

    private static final long EXPECTED_INTERVAL_NANOS = 50_000_000L;
    private static final long MAX_ACCEPTED_GAP_NANOS = 2_000_000_000L;

    public TimerCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Timer");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            data.resetTimerState();
            return;
        }
        if (plugin.isPacketPipelineActive() && frame.getSource() == MovementFrame.Source.BUKKIT_MOVE_EVENT) {
            return;
        }

        long nowNanos = frame.getTimestampNanos();
        long lastPacketNanos = data.getTimerLastPacketNanos();
        if (lastPacketNanos == 0L) {
            data.setTimerLastPacketNanos(nowNanos);
            data.updateTimerRotation(frame.getYaw(), frame.getPitch());
            return;
        }

        long elapsed = nowNanos - lastPacketNanos;
        data.setTimerLastPacketNanos(nowNanos);

        if (elapsed <= 0L || elapsed > MAX_ACCEPTED_GAP_NANOS) {
            data.resetTimerState();
            data.setTimerLastPacketNanos(nowNanos);
            data.updateTimerRotation(frame.getYaw(), frame.getPitch());
            return;
        }

        double maxNegativeBalance = plugin.getConfig().getDouble("checks.Timer.max-credit-ms", -250.0D);
        double flagThreshold = plugin.getConfig().getDouble("checks.Timer.balance-threshold-ms", 65.0D);
        double idlePacketWeight = clamp(plugin.getConfig().getDouble("checks.Timer.idle-packet-weight", 0.0D), 0.0D,
                1.0D);
        double lookOnlyWeight = clamp(plugin.getConfig().getDouble("checks.Timer.look-only-weight", 0.35D), 0.0D,
                1.0D);
        double idleDecayMultiplier = clamp(plugin.getConfig().getDouble("checks.Timer.idle-decay-multiplier", 0.82D),
                0.0D, 1.0D);
        double rotationThreshold = Math.max(0.0D,
                plugin.getConfig().getDouble("checks.Timer.look-rotation-threshold", 0.5D));
        long minimumLookSampleNanos = (long) (Math.max(0.0D,
                plugin.getConfig().getDouble("checks.Timer.minimum-look-sample-ms", 8.0D)) * 1_000_000.0D);

        float lastYaw = data.isTimerRotationInitialized() ? data.getTimerLastYaw() : frame.getYaw();
        float lastPitch = data.isTimerRotationInitialized() ? data.getTimerLastPitch() : frame.getPitch();
        double yawDelta = wrappedAngleDistance(frame.getYaw(), lastYaw);
        double pitchDelta = Math.abs(frame.getPitch() - lastPitch);
        boolean meaningfulLook = frame.hasLook() && (yawDelta >= rotationThreshold || pitchDelta >= rotationThreshold);

        double balance = data.getTimerBalance();
        double positiveWeight = frame.hasPosition() ? 1.0D : (meaningfulLook ? lookOnlyWeight : idlePacketWeight);
        if (!frame.hasPosition() && elapsed < minimumLookSampleNanos) {
            positiveWeight = Math.min(positiveWeight, idlePacketWeight);
        }

        double advantageMs = (EXPECTED_INTERVAL_NANOS - elapsed) / 1_000_000.0D;
        if (positiveWeight <= 0.0D) {
            balance *= idleDecayMultiplier;
            if (advantageMs < 0.0D) {
                balance += advantageMs;
            }
            if (balance < maxNegativeBalance) {
                balance = maxNegativeBalance;
            }
            data.setTimerBalance(balance);
            data.updateTimerRotation(frame.getYaw(), frame.getPitch());
            return;
        }

        if (advantageMs > 0.0D) {
            advantageMs *= positiveWeight;
        }
        balance += advantageMs;
        if (balance < maxNegativeBalance) {
            balance = maxNegativeBalance;
        }
        data.setTimerBalance(balance);

        double jitterMs = data.getTransactionRttJitterNanos() / 1_000_000.0D;
        double jitterCredit = Math.min(jitterMs * 0.5D, 20.0D);
        double effectiveThreshold = flagThreshold + jitterCredit;

        if (balance > effectiveThreshold) {
            double severity = balance / effectiveThreshold;
            double timerSpeed = elapsed > 0L ? (double) EXPECTED_INTERVAL_NANOS / (double) elapsed : 99.0D;

            flag(player, data, Math.min(severity, 3.0D),
                    "balance=" + String.format(Locale.ROOT, "%.1fms", balance)
                            + " speed=" + String.format(Locale.ROOT, "%.3fx", timerSpeed)
                            + " weight=" + String.format(Locale.ROOT, "%.2f", positiveWeight)
                            + " jitter=" + String.format(Locale.ROOT, "%.1fms", jitterMs)
                            + " src=" + frame.getSource().name());

            data.setTimerBalance(balance * 0.35D);
        }

        if (frame.hasPosition()) {
            int maxMoves = plugin.getConfig().getInt("checks.Timer.max-moves-per-second", 26);
            if (data.getMoveWindow() > maxMoves) {
                double buffer = increaseBuffer(data, 0.5D);
                if (buffer > plugin.getConfig().getDouble("checks.Timer.buffer", 2.0D)) {
                    flag(player, data, 0.5D, "legacy-count moves=" + data.getMoveWindow());
                }
            }
        }

        data.updateTimerRotation(frame.getYaw(), frame.getPitch());
    }

    private double wrappedAngleDistance(float current, float previous) {
        double delta = Math.abs(current - previous);
        if (delta > 180.0D) {
            delta = 360.0D - delta;
        }
        return delta;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
