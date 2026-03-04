package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class KnockbackHandlerLegacy extends Check {
    public KnockbackHandlerLegacy(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Knockback");
    }

    public void onVelocityPacket(Player player, PlayerData data, int entityId, int vx, int vy, int vz, long sentAtNanos) {
        if (!isEnabled()) {
            return;
        }
        if (entityId != player.getEntityId()) {
            return;
        }

        short preTxId = 0;
        short postTxId = 0;
        if (plugin.transactionSync() != null) {
            preTxId = plugin.transactionSync().sendTransactionNow(player);
            postTxId = plugin.transactionSync().sendTransactionNow(player);
        }

        boolean setbackLike = data.getLastSafeLocation() == null;
        long txWindowMaxMs = plugin.getConfig().getLong("checks.Knockback.tx-window-max-ms", 500L);
        data.startKnockbackSample(sentAtNanos, entityId, preTxId, postTxId,
            vx / 8000.0D, vy / 8000.0D, vz / 8000.0D, setbackLike, txWindowMaxMs);

        Vector velocity = new Vector(vx / 8000.0D, vy / 8000.0D, vz / 8000.0D);
        int windowTicks = plugin.getConfig().getInt("checks.Knockback.window-ticks", 8);
        data.armVelocityWindow(velocity, windowTicks);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data, false)) {
            return;
        }

        if (!data.hasPredictionForFrame(frame.getTimestampNanos())) {
            return;
        }

        PlayerData.KnockbackSample likely = data.getLikelyKB();
        PlayerData.KnockbackSample firstBread = data.getFirstBreadKB();
        if (likely == null && firstBread == null) {
            return;
        }

        double offset = data.getPredictionReducedDeviation();
        if (offset < 0.0D) {
            offset = 0.0D;
        }
        data.updateKnockbackOffset(offset);

        int delayedWindow = Math.max(1, plugin.getConfig().getInt("checks.Knockback.delayed-window-ticks", 3));
        double expectedXZ = likely != null ? likely.horizontalMagnitude() : firstBread.horizontalMagnitude();
        double responseThreshold = Math.max(0.03D, expectedXZ * 0.20D);
        double observedXZ = data.getLastDeltaXZ();
        data.recordKnockbackObservedMotion(observedXZ, responseThreshold, delayedWindow);

        if (likely == null) {
            return;
        }

        if (likely.getTicksObserved() < Math.max(1, plugin.getConfig().getInt("checks.Knockback.min-samples", 2))) {
            return;
        }

        double threshold = plugin.getConfig().getDouble("checks.Knockback.threshold", 0.001D);
        if (likely.getOffset() <= threshold) {
            data.decayKnockbackScore(plugin.getConfig().getDouble("checks.Knockback.setback-decay-multiplier", 0.999D));
            data.completeCurrentKnockbackSample();
            return;
        }

        if (likely.isDelayedPattern()) {
            data.addKnockbackScore(0.35D);
        }

        data.addKnockbackScore(likely.getOffset());
        double ceiling = plugin.getConfig().getDouble("checks.Knockback.max-ceiling", 4.0D);
        if (data.getKnockbackOffset() > ceiling) {
            data.setKnockbackOffset(ceiling);
        }

        double immediate = plugin.getConfig().getDouble("checks.Knockback.immediate-setback-threshold", 0.10D);
        double maxAdvantage = plugin.getConfig().getDouble("checks.Knockback.max-advantage", 1.0D);
        double offsetToFlag = plugin.getConfig().getDouble("checks.Knockback.threshold", 0.001D);

        if (likely.getOffset() > offsetToFlag) {
            double score = likely.getOffset();
            flag(player, data, score,
                "o=" + fmt(likely.getOffset())
                    + " tx=" + likely.getPostTransactionId()
                    + " delayPattern=" + likely.isDelayedPattern()
                    + " ticks=" + likely.getTicksObserved());

            if (likely.isSetbackLike() || likely.getOffset() >= immediate || data.getKnockbackOffset() >= maxAdvantage) {
                if (plugin.getConfig().getBoolean("checks.Knockback.setback", true) && data.getLastSafeLocation() != null) {
                    player.teleport(data.getLastSafeLocation());
                }
            }
        }

        data.completeCurrentKnockbackSample();
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
