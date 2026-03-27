package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.CandidateVelocity;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionEvaluation;
import ac.grim.legacyac.prediction.PredictionUncertaintyHandler;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Packet-first prediction built around one frame budget plus a very small
 * 1.7-specific correction layer.
 */
public final class PredictionMovementCheck extends Check {
    private static final String ADVANTAGE_KEY = "Prediction.advantage";

    public PredictionMovementCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Prediction");
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        PlayerData.MovementStateSnapshot state = data.getMovementStateSnapshot();
        if (!state.isTeleportAligned()) {
            return;
        }

        long now = System.nanoTime();
        long packetAgeNanos = now - data.getLastRawMovementPacketAt();
        long maxPacketAgeNanos = plugin.getConfig().getLong("prediction.max-packet-age-nanos", 120000000L);
        if (data.getLastRawMovementPacketAt() != 0L && packetAgeNanos > maxPacketAgeNanos) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double deltaY = data.getLastDeltaY();
        double minMovingHorizontal = plugin.getConfig().getDouble("prediction.min-moving-horizontal", 0.03D);
        double minMovingVertical = plugin.getConfig().getDouble("prediction.min-moving-vertical", 0.03D);
        if (horizontal < minMovingHorizontal && Math.abs(deltaY) < minMovingVertical) {
            decayAdvantage(data);
            data.markPredictionReady(frame.getTimestampNanos());
            return;
        }

        int blockX = (int) Math.floor(to.getX());
        int blockY = (int) Math.floor(to.getY());
        int blockZ = (int) Math.floor(to.getZ());
        Material feet = data.getCompensatedBlockType(player, blockX, blockY, blockZ);
        Material below = data.getCompensatedBlockType(player, blockX, blockY - 1, blockZ);
        PlayerData.PredictionContext context = data.getPredictionContext();
        boolean recentTowerPlace = data.hasRecentPlacedBlock(400L);
        int playerBlockX = (int) Math.floor(to.getX());
        int playerBlockY = (int) Math.floor(to.getY());
        int playerBlockZ = (int) Math.floor(to.getZ());
        boolean placedUnderSelf = recentTowerPlace
                && Math.abs(data.getLastPlacedBlockX() - playerBlockX) <= 1
                && Math.abs(data.getLastPlacedBlockZ() - playerBlockZ) <= 1
                && data.getLastPlacedBlockY() >= playerBlockY - 2
                && data.getLastPlacedBlockY() <= playerBlockY;
        boolean towerLike = placedUnderSelf && Math.abs(deltaY) > 0.28D && horizontal < 0.55D;
        if (!towerLike && frameContext != null && !frameContext.getPendingBlockChanges().isEmpty()) {
            towerLike = Math.abs(deltaY) > 0.28D && horizontal < 0.55D;
        }
        boolean customSpeedBurst = Math.abs(player.getWalkSpeed() - 0.2F) > 1.0E-4F && horizontal > 0.35D;
        if ((context.isRecentTeleport() && (horizontal > 1.25D || Math.abs(deltaY) > 0.90D))
                || towerLike || customSpeedBurst) {
            decayAdvantage(data);
            data.markPredictionReady(frame.getTimestampNanos());
            return;
        }

        int highFallRecoveryTicks = plugin.getConfig().getInt("prediction.recovery-after-high-fall-ticks", 8);
        ToleranceBudgetEngine.BudgetSnapshot budget = frameContext != null ? frameContext.getBudgetSnapshot() : getBudget(data);
        double predictionAllowance = budget != null ? budget.getMovementAllowance()
                : plugin.getConfig().getDouble("prediction.budget.base", 0.060D);
        double legacyCorrection = PredictionUncertaintyHandler.resolveLegacyCorrection(context, data, state, plugin, deltaY);

        double observedMotionX = data.getShadowMotionX();
        double observedMotionZ = data.getShadowMotionZ();
        double prevMotionX = data.getPrevShadowMotionX();
        double prevMotionZ = data.getPrevShadowMotionZ();
        boolean hasVectorData = data.isShadowInitialized()
                && (Math.abs(observedMotionX) > 1.0E-10D || Math.abs(observedMotionZ) > 1.0E-10D || horizontal > 0.001D);

        PredictionEvaluation evaluation;
        float packetYaw = to.getYaw();
        if (hasVectorData) {
            evaluation = LegacyPredictionEngine.evaluateBestCandidateVector(
                    player, feet, below,
                    data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                    data.wasOnGround(), context, highFallRecoveryTicks,
                    observedMotionX, observedMotionZ, deltaY,
                    prevMotionX, prevMotionZ,
                    0.0D, packetYaw);
        } else {
            evaluation = LegacyPredictionEngine.evaluateBestCandidate(
                    player, feet, below,
                    data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                    data.wasOnGround(), context, highFallRecoveryTicks,
                    horizontal, deltaY,
                    0.0D, packetYaw);
        }

        CandidateVelocity bestCandidate = evaluation.getBestCandidate();
        double rawOffset = evaluation.getRawOffset();
        data.setPredictionMinDeviation(rawOffset);

        double horizontalDeviation = 0.0D;
        if (bestCandidate != null) {
            if (hasVectorData) {
                double cdx = observedMotionX - bestCandidate.getMotionX();
                double cdz = observedMotionZ - bestCandidate.getMotionZ();
                horizontalDeviation = Math.sqrt(cdx * cdx + cdz * cdz);
            } else {
                horizontalDeviation = Math.max(0.0D, horizontal - bestCandidate.getHorizontalMagnitude());
            }
        }
        data.setPredictionHorizontalDeviation(horizontalDeviation);

        double reducedOffset = Math.max(0.0D, rawOffset - predictionAllowance - legacyCorrection);
        double reducedHorizontalDeviation = Math.max(0.0D, horizontalDeviation - predictionAllowance - legacyCorrection);
        data.setPredictionReducedDeviation(reducedOffset);
        data.setPredictionReducedHorizontalDeviation(reducedHorizontalDeviation);
        data.setPredictionBestProfile(bestCandidate == null ? "none" : bestCandidate.getProfile());
        data.markPredictionReady(frame.getTimestampNanos());

        double threshold = plugin.getConfig().getDouble("Simulation.threshold", 0.005D);
        double immediateSetbackThreshold = plugin.getConfig().getDouble("Simulation.immediate-setback-threshold", 0.1D);
        double maxAdvantage = plugin.getConfig().getDouble("Simulation.max-advantage", 1.0D);
        double maxCeiling = plugin.getConfig().getDouble("Simulation.max-ceiling", 4.0D);
        double offset = reducedOffset;
        String bestProfile = bestCandidate == null ? "none" : bestCandidate.getProfile();

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " [Prediction] rawOffset=" + fmt(rawOffset)
                    + " allowance=" + fmt(predictionAllowance)
                    + " correction=" + fmt(legacyCorrection)
                    + " reduced=" + fmt(reducedOffset)
                    + " offset=" + fmt(offset)
                    + " threshold=" + fmt(threshold)
                    + " advantage=" + fmt(data.getBuffer(ADVANTAGE_KEY))
                    + " maxAdv=" + fmt(maxAdvantage)
                    + " h=" + fmt(horizontal)
                    + " dY=" + fmt(deltaY)
                    + " blocker=" + state.getPrimaryBlocker().name()
                    + " pending=" + state.getPendingChanges()
                    + " best=" + bestProfile);
        }

        if (offset >= threshold || offset >= immediateSetbackThreshold) {
            double advantage = data.addBuffer(ADVANTAGE_KEY, offset);
            advantage = Math.min(advantage, maxCeiling);
            data.setBuffer(ADVANTAGE_KEY, advantage);

            recordEvidence(data, offset, "PREDICTION_MODEL");

            String humanOffset;
            if (offset < 0.001D) {
                humanOffset = String.format(Locale.ROOT, "%.4E", offset).replace("E-0", "E-");
            } else {
                humanOffset = String.format(Locale.ROOT, "%6f", offset).replace("0.", ".");
            }

            if (advantage >= maxAdvantage || offset >= immediateSetbackThreshold) {
                flag(player, data, offset, humanOffset
                        + " adv=" + fmt(advantage)
                        + " best=" + bestProfile
                        + " h=" + fmt(horizontal)
                        + " dY=" + fmt(deltaY));
            } else {
                plugin.alerts().alert(player, getName(), data.getViolation(getName()),
                        humanOffset + " adv=" + fmt(advantage) + " best=" + bestProfile);
            }
        } else {
            decayAdvantage(data);
        }
    }

    private void decayAdvantage(PlayerData data) {
        double decayMultiplier = plugin.getConfig().getDouble("Simulation.setback-decay-multiplier", 0.999D);
        data.scaleBuffer(ADVANTAGE_KEY, decayMultiplier);
        double linearDecay = plugin.getConfig().getDouble("Simulation.linear-decay-per-tick", 0.05D);
        double current = data.getBuffer(ADVANTAGE_KEY);
        if (current > 0.0D) {
            data.setBuffer(ADVANTAGE_KEY, Math.max(0.0D, current - linearDecay));
        }
        coolDownScore(data);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
