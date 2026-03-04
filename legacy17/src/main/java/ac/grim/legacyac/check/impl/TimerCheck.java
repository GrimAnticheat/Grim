package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.entity.Player;

/**
 * Timer — Nanosecond-precision real-time-clock timer check.
 * Ported from Grim's Timer check design.
 *
 * <p>Instead of simply counting packets per second (which can't detect 1.005x timer),
 * this tracks the time advantage the client accumulates by comparing expected vs actual
 * packet intervals. Each movement packet should arrive at ~50ms intervals (20 TPS).
 * If the client sends packets faster, it accumulates a positive "advantage" balance.
 * Once that balance exceeds a threshold, we flag.</p>
 *
 * <p>Transaction RTT is used for lag compensation — we grant the client credit
 * for network jitter so legitimate lag spikes don't cause false positives.</p>
 */
public final class TimerCheck extends Check {

    /** Expected interval between movement packets in nanoseconds (50ms = 1 tick at 20TPS) */
    private static final long EXPECTED_INTERVAL_NANOS = 50_000_000L;

    public TimerCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Timer");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            data.resetTimerState();
            return;
        }

        long nowNanos = frame.getTimestampNanos();
        long lastPacketNanos = data.getTimerLastPacketNanos();

        // First packet — initialize baseline
        if (lastPacketNanos == 0L) {
            data.setTimerLastPacketNanos(nowNanos);
            return;
        }

        long elapsed = nowNanos - lastPacketNanos;
        data.setTimerLastPacketNanos(nowNanos);

        // Ignore absurdly large gaps (server freeze, rejoin, etc.)
        if (elapsed > 2_000_000_000L || elapsed < 0L) {
            data.resetTimerState();
            return;
        }

        // Calculate advantage: how much faster than expected this packet arrived
        // Positive = client is sending faster than expected (timer cheat)
        // Negative = client is sending slower than expected (lag or slow timer)
        long advantage = EXPECTED_INTERVAL_NANOS - elapsed;
        double advantageMs = advantage / 1_000_000.0;

        // Accumulate balance
        double balance = data.getTimerBalance() + advantageMs;

        // Lag compensation: allow the client to bank negative balance (credit for lag)
        // but cap how negative it can go, so they can't stockpile credit
        double maxNegativeBalance = plugin.getConfig().getDouble("checks.Timer.max-credit-ms", -200.0);
        if (balance < maxNegativeBalance) {
            balance = maxNegativeBalance;
        }

        // Additional RTT-based compensation: grant extra credit proportional to jitter
        double jitterMs = data.getTransactionRttJitterNanos() / 1_000_000.0;
        double jitterCredit = Math.min(jitterMs * 0.5, 15.0);

        data.setTimerBalance(balance);

        // Flag when balance exceeds threshold (client has sent packets faster than real time)
        double flagThreshold = plugin.getConfig().getDouble("checks.Timer.balance-threshold-ms", 50.0);

        // Apply jitter credit to threshold
        double effectiveThreshold = flagThreshold + jitterCredit;

        if (balance > effectiveThreshold) {
            double severity = balance / effectiveThreshold;
            double timerSpeed = elapsed > 0
                    ? (double) EXPECTED_INTERVAL_NANOS / elapsed
                    : 99.0;

            flag(player, data, Math.min(severity, 3.0),
                    "balance=" + String.format("%.1fms", balance)
                            + " speed=" + String.format("%.3fx", timerSpeed)
                            + " jitter=" + String.format("%.1fms", jitterMs));

            // Drain some balance after flagging to prevent spam
            data.setTimerBalance(balance * 0.5);
        }

        // Also keep the legacy per-second counter for backwards compatibility with config
        int maxMoves = plugin.getConfig().getInt("checks.Timer.max-moves-per-second", 26);
        if (data.getMoveWindow() > maxMoves) {
            double buffer = increaseBuffer(data, 0.5);
            if (buffer > plugin.getConfig().getDouble("checks.Timer.buffer", 2.0)) {
                flag(player, data, 0.5, "legacy-count moves=" + data.getMoveWindow());
            }
        }
    }
}
