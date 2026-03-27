package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.evidence.CombatEvidence;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public final class VelocityCheck extends Check {
    public VelocityCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Velocity");
    }

    @Override
    protected boolean isEnabled() {
        return getMergedBoolean("enabled", true);
    }

    @Override
    protected double getMaxViolation() {
        return getMergedDouble("max-vl", 10.0D);
    }

    public void onVelocity(PlayerVelocityEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }
        armWindow(data, velocity);
    }

    public void onVelocityPacket(Player player, PlayerData data, int entityId, int vx, int vy, int vz, long sentAtNanos) {
        if (!isEnabled() || entityId != player.getEntityId()) {
            return;
        }

        Vector velocity = new Vector(vx / 8000.0D, vy / 8000.0D, vz / 8000.0D);
        armWindow(data, velocity);

        short preTxId = 0;
        short postTxId = 0;
        if (plugin.transactionSync() != null) {
            preTxId = plugin.transactionSync().sendTransactionNow(player);
            postTxId = plugin.transactionSync().sendTransactionNow(player);
        }
        data.recordPendingVelocityChange(postTxId);

        long txWindowMaxMs = getMergedLong("tx-window-max-ms", 500L);
        data.startVelocitySample(sentAtNanos, preTxId, postTxId,
                velocity.getX(), velocity.getY(), velocity.getZ(), txWindowMaxMs);

        boolean setbackLike = data.getLastSafeLocation() == null;
        data.startKnockbackSample(sentAtNanos, entityId, preTxId, postTxId,
                velocity.getX(), velocity.getY(), velocity.getZ(), setbackLike, txWindowMaxMs);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (handleKnockbackSample(player, frame, data)) {
            return;
        }

        handleVelocityWindow(player, data);
    }

    private boolean handleKnockbackSample(Player player, MovementFrame frame, PlayerData data) {
        if (!data.hasPredictionForFrame(frame.getTimestampNanos())) {
            return false;
        }

        PlayerData.KnockbackSample likely = data.getLikelyKB();
        PlayerData.KnockbackSample firstBread = data.getFirstBreadKB();
        PlayerData.VelocitySample velocitySample = data.getCurrentVelocitySample();
        if (likely == null && firstBread == null && velocitySample == null) {
            return false;
        }

        double offset = Math.max(0.0D, data.getPredictionReducedDeviation());
        data.updateKnockbackOffset(offset);
        if (velocitySample != null) {
            velocitySample.observeTick(offset);
        }

        int delayedWindow = getDelayedWindowTicks();
        double expectedXZ = 0.0D;
        if (likely != null) {
            expectedXZ = likely.horizontalMagnitude();
        } else if (firstBread != null) {
            expectedXZ = firstBread.horizontalMagnitude();
        } else {
            expectedXZ = data.getExpectedVelocityXZ();
        }

        double responseThreshold = Math.max(0.03D, expectedXZ * 0.20D);
        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        if (budget != null) {
            responseThreshold += budget.getVelocityResponseSlack();
        }

        double observedXZ = data.getLastDeltaXZ();
        data.recordKnockbackObservedMotion(observedXZ, responseThreshold, delayedWindow);
        if (velocitySample != null) {
            velocitySample.recordObservedMotion(observedXZ, responseThreshold, delayedWindow);
        }

        if (likely == null) {
            return false;
        }

        int minSamples = getMergedInt("min-samples", 2);
        if (likely.getTicksObserved() < Math.max(1, minSamples)) {
            return false;
        }

        double threshold = getMergedDouble("threshold", 0.001D);
        if (likely.getOffset() <= threshold) {
            data.decayKnockbackScore(getMergedDouble("setback-decay-multiplier", 0.999D));
            coolDownScore(data);
            data.completeCurrentKnockbackSample();
            data.clearVelocityWindow();
            return true;
        }

        double score = likely.getOffset();
        if (likely.isDelayedPattern()) {
            data.addKnockbackScore(0.35D);
            score += 0.35D;
        }
        data.addKnockbackScore(likely.getOffset());

        double ceiling = getMergedDouble("max-ceiling", 4.0D);
        if (data.getKnockbackOffset() > ceiling) {
            data.setKnockbackOffset(ceiling);
        }

        double buffer = slideAndAddScore(data, score, getMergedDouble("window-weight", 1.0D));
        double immediate = getMergedDouble("immediate-setback-threshold", 0.10D);
        double maxAdvantage = getMergedDouble("max-advantage", 1.0D);
        double minScoreToFlag = getMergedDouble("min-score-to-flag", getMergedDouble("buffer", 1.2D));

        String detail = "kb-o=" + fmt(likely.getOffset())
                + " tx=" + likely.getPostTransactionId()
                + " delayPattern=" + likely.isDelayedPattern()
                + " ticks=" + likely.getTicksObserved()
                + " adv=" + fmt(data.getKnockbackOffset())
                + " buf=" + fmt(buffer);

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " Velocity(KB) " + detail);
        }

        boolean shouldFlag = score >= minScoreToFlag
                || likely.getOffset() >= immediate
                || data.getKnockbackOffset() >= maxAdvantage;
        if (shouldFlag) {
            flag(player, data, score, detail);
            recordVelocityCombatEvidence(player, data, score, detail);

            if ((likely.isSetbackLike() || likely.getOffset() >= immediate || data.getKnockbackOffset() >= maxAdvantage)
                    && getMergedBoolean("setback", true)
                    && data.getLastSafeLocation() != null) {
                player.teleport(data.getLastSafeLocation());
            }
        }

        data.completeCurrentKnockbackSample();
        data.clearVelocityWindow();
        return true;
    }

    private void handleVelocityWindow(Player player, PlayerData data) {
        if (data.hasPendingVelocityWindow()) {
            return;
        }
        if (!data.hasCompletedVelocityWindow()) {
            return;
        }

        double expectedXZ = data.getExpectedVelocityXZ();
        double expectedY = data.getExpectedVelocityY();
        double observedXZ = data.getObservedVelocityXZ();
        double observedY = data.getObservedVelocityY();
        data.clearVelocityWindow();

        double minExpectedXZ = getMergedDouble("min-expected-xz", 0.08D);
        if ((expectedXZ < minExpectedXZ && expectedY < 0.08D) || player.getVehicle() != null || data.isTeleportSyncPending()) {
            coolDownScore(data);
            return;
        }

        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        double slack = budget != null ? budget.getVelocityResponseSlack() : 0.0D;
        double requiredXZ = getMergedDouble("min-response-ratio-xz", 0.40D);
        double requiredY = getMergedDouble("min-response-ratio-y", 0.25D);

        if (data.getPredictionBestProfile().contains("wall")) {
            requiredXZ = Math.max(0.25D, requiredXZ - 0.10D);
        }
        if (data.getSpeedLevel() > 0) {
            requiredXZ = Math.max(0.25D, requiredXZ - Math.min(0.08D, data.getSpeedLevel() * 0.03D));
        }

        double effectiveExpectedXZ = Math.max(0.01D, expectedXZ - slack);
        double effectiveExpectedY = expectedY <= 0.05D ? 0.0D : Math.max(0.01D, expectedY - (slack * 0.75D));
        double ratioXZ = Math.min(1.25D, observedXZ / effectiveExpectedXZ);
        double ratioY = effectiveExpectedY <= 0.0D ? 1.0D : Math.min(1.25D, observedY / effectiveExpectedY);

        double deficitXZ = Math.max(0.0D, requiredXZ - ratioXZ);
        double deficitY = Math.max(0.0D, requiredY - ratioY);
        if (deficitXZ <= 0.0D && deficitY <= 0.0D) {
            coolDownScore(data);
            return;
        }

        double score = (deficitXZ * 2.4D) + (deficitY * 1.6D);
        if (ratioXZ < 0.05D && expectedXZ >= 0.18D) {
            score += 0.25D;
        }
        double buffer = slideAndAddScore(data, score, getMergedDouble("window-weight", 1.0D));

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Velocity expectedXZ=" + fmt(expectedXZ)
                    + " observedXZ=" + fmt(observedXZ)
                    + " ratioXZ=" + fmt(ratioXZ)
                    + " expectedY=" + fmt(expectedY)
                    + " observedY=" + fmt(observedY)
                    + " ratioY=" + fmt(ratioY)
                    + " score=" + fmt(score)
                    + " buffer=" + fmt(buffer));
        }

        double minScoreToFlag = getMergedDouble("min-score-to-flag", getMergedDouble("buffer", 1.2D));
        if (buffer > minScoreToFlag) {
            String detail = "ratioXZ=" + fmt(ratioXZ)
                    + " ratioY=" + fmt(ratioY)
                    + " expectedXZ=" + fmt(expectedXZ)
                    + " observedXZ=" + fmt(observedXZ)
                    + " expectedY=" + fmt(expectedY)
                    + " observedY=" + fmt(observedY);
            flag(player, data, score, detail);
            recordVelocityCombatEvidence(player, data, score, detail);
        }
    }

    private void armWindow(PlayerData data, Vector velocity) {
        int ticks = getMergedInt("window-ticks", 8);
        data.armVelocityWindow(velocity, ticks);
        data.setLastVelocityAt(System.currentTimeMillis());
        data.setLastVelocityXZ(Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ()));
    }

    private int getDelayedWindowTicks() {
        if (plugin.getConfig().isSet("checks.Velocity.delayed-window-ticks")) {
            return Math.max(1, plugin.getConfig().getInt("checks.Velocity.delayed-window-ticks"));
        }
        if (plugin.getConfig().isSet("checks.Velocity.delayed-kb-ticks")) {
            return Math.max(1, plugin.getConfig().getInt("checks.Velocity.delayed-kb-ticks"));
        }
        if (plugin.getConfig().isSet("checks.Knockback.delayed-window-ticks")) {
            return Math.max(1, plugin.getConfig().getInt("checks.Knockback.delayed-window-ticks"));
        }
        return 3;
    }

    private double getMergedDouble(String key, double defaultValue) {
        String velocityPath = "checks.Velocity." + key;
        if (plugin.getConfig().isSet(velocityPath)) {
            return plugin.getConfig().getDouble(velocityPath);
        }
        String knockbackPath = "checks.Knockback." + key;
        if (plugin.getConfig().isSet(knockbackPath)) {
            return plugin.getConfig().getDouble(knockbackPath);
        }
        return defaultValue;
    }

    private int getMergedInt(String key, int defaultValue) {
        String velocityPath = "checks.Velocity." + key;
        if (plugin.getConfig().isSet(velocityPath)) {
            return plugin.getConfig().getInt(velocityPath);
        }
        String knockbackPath = "checks.Knockback." + key;
        if (plugin.getConfig().isSet(knockbackPath)) {
            return plugin.getConfig().getInt(knockbackPath);
        }
        return defaultValue;
    }

    private long getMergedLong(String key, long defaultValue) {
        String velocityPath = "checks.Velocity." + key;
        if (plugin.getConfig().isSet(velocityPath)) {
            return plugin.getConfig().getLong(velocityPath);
        }
        String knockbackPath = "checks.Knockback." + key;
        if (plugin.getConfig().isSet(knockbackPath)) {
            return plugin.getConfig().getLong(knockbackPath);
        }
        return defaultValue;
    }

    private boolean getMergedBoolean(String key, boolean defaultValue) {
        String velocityPath = "checks.Velocity." + key;
        if (plugin.getConfig().isSet(velocityPath)) {
            return plugin.getConfig().getBoolean(velocityPath);
        }
        String knockbackPath = "checks.Knockback." + key;
        if (plugin.getConfig().isSet(knockbackPath)) {
            return plugin.getConfig().getBoolean(knockbackPath);
        }
        return defaultValue;
    }

    private void recordVelocityCombatEvidence(Player player, PlayerData data, double score, String detail) {
        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        CombatEvidence evidence = CombatEvidence.builder(
                CombatEvidence.CombatCheckType.VELOCITY, player.getName(), "")
                .localAttackTime(System.currentTimeMillis())
                .directDistance(score)
                .scoring(score,
                        getMergedDouble("min-score-to-flag", getMergedDouble("buffer", 1.2D)),
                        true)
                .detail(detail)
                .frameLink(frameContext == null ? -1L : frameContext.getFrameId(),
                        frameContext == null ? -1 : frameContext.getTxWindowId())
                .build();
        data.recordCombatEvidence(evidence);
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
