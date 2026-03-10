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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public final class VelocityCheck extends Check {
    public VelocityCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Knockback");
    }

    public void onVelocity(PlayerVelocityEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }
        int ticks = plugin.getConfig().getInt("checks.Knockback.window-ticks", 8);
        data.armVelocityWindow(velocity, ticks);
    }

    public void onVelocityPacket(Player player, PlayerData data, int entityId, int vx, int vy, int vz, long sentAtNanos) {
        if (!isEnabled() || entityId != player.getEntityId()) {
            return;
        }
        short preTxId = 0;
        short postTxId = 0;
        if (plugin.transactionSync() != null) {
            preTxId = plugin.transactionSync().sendTransactionNow(player);
            postTxId = plugin.transactionSync().sendTransactionNow(player);
        }
        double velocityX = vx / 8000.0D;
        double velocityY = vy / 8000.0D;
        double velocityZ = vz / 8000.0D;
        long txWindowMaxMs = plugin.getConfig().getLong("checks.Knockback.tx-window-max-ms", 500L);
        data.startVelocitySample(sentAtNanos, preTxId, postTxId, velocityX, velocityY, velocityZ, txWindowMaxMs);
        data.armVelocityWindow(new Vector(velocityX, velocityY, velocityZ), plugin.getConfig().getInt("checks.Knockback.window-ticks", 8));
        data.setLastVelocityAt(System.currentTimeMillis());
        data.setLastVelocityXZ(Math.sqrt(velocityX * velocityX + velocityZ * velocityZ));
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(),
                event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(),
                true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data, false)) {
            return;
        }
        PlayerData.VelocitySample sample = data.getCurrentVelocitySample();
        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        boolean oldPredictionReady = data.hasPredictionForFrame(frame.getTimestampNanos());
        boolean predictionReady = frameContext != null && frameContext.getPredictionOutput() != null
                ? frameContext.getPredictionOutput().isReady()
                : oldPredictionReady;
        if (plugin.getConfig().getBoolean("pipeline.frame-context.dual-track-log", true)
                && oldPredictionReady != predictionReady) {
            plugin.getLogger().info("[GLAC-FRAMECTX-DIFF] " + player.getName()
                    + " check=Velocity oldReady=" + oldPredictionReady + " newReady=" + predictionReady
                    + " frame=" + frame.getTimestampNanos());
        }
        if (sample == null || !predictionReady) {
            return;
        }

        double expectedXZ = Math.sqrt(sample.getVx() * sample.getVx() + sample.getVz() * sample.getVz());
        double expectedY = Math.abs(sample.getVy());
        double observedXZ = data.getLastDeltaXZ();
        double observedY = Math.abs(data.getLastDeltaY());
        double offset = Math.abs(observedXZ - expectedXZ) + Math.abs(observedY - expectedY);

        if (sample.hasFlag(PlayerData.VelocitySample.FLAG_PRE_ACK)
                && !sample.hasFlag(PlayerData.VelocitySample.FLAG_POST_ACK)) {
            sample.observeTick(offset);
            double responseThreshold = Math.max(0.03D, expectedXZ * 0.20D);
            ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
            if (budget != null) {
                responseThreshold += budget.getVelocityResponseSlack();
            }
            int delayedKbTicks = plugin.getConfig().getInt("checks.Knockback.delayed-window-ticks",
                    plugin.getConfig().getInt("checks.Knockback.window-ticks", 3));
            sample.recordObservedMotion(observedXZ, responseThreshold, delayedKbTicks);
        }

        double minScoreToFlag = plugin.getConfig().getDouble("checks.Knockback.min-score-to-flag",
                plugin.getConfig().getDouble("checks.Knockback.buffer", 1.2D));
        int minSamples = plugin.getConfig().getInt("checks.Knockback.min-samples", 2);

        if (!sample.hasFlag(PlayerData.VelocitySample.FLAG_LIKELY_CONFIRMED) || sample.getTicksObserved() < minSamples) {
            return;
        }

        double predictionReduced = frameContext != null && frameContext.getPredictionOutput() != null
                ? frameContext.getPredictionOutput().getReducedDeviation()
                : data.getPredictionReducedDeviation();
        double likelyStageScore = Math.max(0.0D, Math.max(sample.getMinOffset(), predictionReduced)
                - plugin.getConfig().getDouble("checks.Knockback.threshold", 0.001D));
        double buffer = slideAndAddScore(data, likelyStageScore,
                plugin.getConfig().getDouble("checks.Knockback.window-weight", 1.0D));

        if (sample.hasFlag(PlayerData.VelocitySample.FLAG_DELAYED_KB_PATTERN)) {
            buffer = slideAndAddScore(data, 0.35D, 1.0D);
        }

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Knockback pre=" + sample.getPreTxId()
                    + " post=" + sample.getPostTxId()
                    + " ticks=" + sample.getTicksObserved()
                    + " minOffset=" + fmt(sample.getMinOffset())
                    + " score=" + fmt(likelyStageScore)
                    + " flags=" + sample.getStateFlags());
        }

        if (buffer > minScoreToFlag) {
            flag(player, data, likelyStageScore,
                    "preTx=" + sample.getPreTxId()
                            + " postTx=" + sample.getPostTxId()
                            + " minOffset=" + fmt(sample.getMinOffset())
                            + " ticks=" + sample.getTicksObserved()
                            + " flags=" + sample.getStateFlags());
            recordKnockbackCombatEvidence(player, data, sample, likelyStageScore);
        }
    }

    private void recordKnockbackCombatEvidence(Player player, PlayerData data,
            PlayerData.VelocitySample sample, double score) {
        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        CombatEvidence evidence = CombatEvidence.builder(
                CombatEvidence.CombatCheckType.VELOCITY, player.getName(), "")
                .localAttackTime(System.currentTimeMillis())
                .directDistance(sample.getMinOffset())
                .scoring(score,
                        plugin.getConfig().getDouble("checks.Knockback.min-score-to-flag",
                                plugin.getConfig().getDouble("checks.Knockback.buffer", 1.2D)),
                        true)
                .detail("preTx=" + sample.getPreTxId() + " postTx=" + sample.getPostTxId()
                        + " ticks=" + sample.getTicksObserved() + " flags=" + sample.getStateFlags())
                .frameLink(frameContext == null ? -1L : frameContext.getFrameId(),
                        frameContext == null ? -1 : frameContext.getTxWindowId())
                .build();
        data.recordCombatEvidence(evidence);
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
