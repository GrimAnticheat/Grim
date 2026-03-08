package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.prediction.CandidateVelocity;
import ac.grim.legacyac.prediction.LegacyPredictionEngine;
import ac.grim.legacyac.prediction.PredictionEvaluation;
import ac.grim.legacyac.prediction.PredictionUncertaintyHandler;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Prediction check aligned with Grim's OffsetHandler (Simulation check).
 *
 * Key design from Grim:
 * - Compute best-fit candidate offset (Euclidean distance between observed and predicted)
 * - If offset >= threshold, accumulate into advantageGained
 * - If advantageGained >= maxAdvantage OR offset >= immediateSetbackThreshold  setback
 * - If offset < threshold, decay advantageGained by setbackDecayMultiplier
 * - No dual old/new scoring only the candidate model
 *
 * Vector-level prediction: When ProtocolLib shadow position tracking is available,
 * we use the actual per-tick motionX/motionZ from packet positions instead of
 * scalar deltaXZ. This eliminates the fundamental imprecision that caused
 * massive false positives in the direction-sampling approach.
 */
public final class PredictionMovementCheck extends Check {
    /** Accumulated advantage mirrors Grim's OffsetHandler.advantageGained */
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
            // Not moving decay advantage, mark prediction ready
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
        boolean recentTowerPlace = data.getLastClientBlockPlacePacketAt() != 0L
                && System.currentTimeMillis() - data.getLastClientBlockPlacePacketAt() <= 250L;
        boolean towerLike = recentTowerPlace && Math.abs(deltaY) > 0.35D && horizontal < 0.35D;
        boolean customSpeedBurst = Math.abs(player.getWalkSpeed() - 0.2F) > 1.0E-4F && horizontal > 0.35D;
        if ((context.isRecentTeleport() && (horizontal > 1.25D || Math.abs(deltaY) > 0.90D)) || towerLike || customSpeedBurst) {
            decayAdvantage(data);
            data.markPredictionReady(frame.getTimestampNanos());
            data.removeOffsetLenience();
            return;
        }
        int highFallRecoveryTicks = plugin.getConfig().getInt("prediction.recovery-after-high-fall-ticks", 8);

        // ── Uncertainty budget ──
        double uncertaintyBudget = PredictionUncertaintyHandler.resolveBudget(context, plugin);

        // ── Vector-level prediction using shadow motion ──
        // When ProtocolLib shadow tracking is available we have real motionX/motionZ
        // from per-packet position deltas, enabling Grim-quality vector comparison.
        double observedMotionX = data.getShadowMotionX();
        double observedMotionY = data.getShadowMotionY();
        double observedMotionZ = data.getShadowMotionZ();
        double prevMotionX = data.getPrevShadowMotionX();
        double prevMotionZ = data.getPrevShadowMotionZ();
        boolean hasVectorData = data.isShadowInitialized()
                && (Math.abs(observedMotionX) > 1e-10 || Math.abs(observedMotionZ) > 1e-10 || horizontal > 0.001);

        // ── Generate candidates and find best fit ──
        PredictionEvaluation evaluation;
        float packetYaw = to.getYaw();

        if (hasVectorData) {
            // Vector-level comparison the precise approach like Grim
            evaluation = LegacyPredictionEngine.evaluateBestCandidateVector(
                    player, feet, below,
                    data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                    data.wasOnGround(), context, highFallRecoveryTicks,
                    observedMotionX, observedMotionZ, deltaY,
                    prevMotionX, prevMotionZ,
                    uncertaintyBudget, packetYaw);
        } else {
            // Fallback: scalar comparison
            evaluation = LegacyPredictionEngine.evaluateBestCandidate(
                    player, feet, below,
                    data.getPrevDeltaY(), data.getPrevDeltaXZ(),
                    data.wasOnGround(), context, highFallRecoveryTicks,
                    horizontal, deltaY,
                    uncertaintyBudget, packetYaw);
        }
        CandidateVelocity bestCandidate = evaluation.getBestCandidate();
        double rawOffset = evaluation.getRawOffset();

        // ── Store deviation data for downstream checks (Speed, etc.) ──
        data.setPredictionMinDeviation(rawOffset);
        double horizontalDeviation = 0.0D;
        if (bestCandidate != null) {
            if (hasVectorData) {
                // Vector deviation: compare actual H components
                double cdx = observedMotionX - bestCandidate.getMotionX();
                double cdz = observedMotionZ - bestCandidate.getMotionZ();
                horizontalDeviation = Math.sqrt(cdx * cdx + cdz * cdz);
            } else {
                horizontalDeviation = Math.max(0.0D, horizontal - bestCandidate.getHorizontalMagnitude());
            }
        }
        data.setPredictionHorizontalDeviation(horizontalDeviation);

        // Use lenience-aware reduction (Grim OffsetHandler: previous tick's offset as extra tolerance)
        double lastHOffset = data.getLastHorizontalOffset();
        double lastVOffset = data.getLastVerticalOffset();
        double reducedOffset = PredictionUncertaintyHandler.reduceOffsetWithLenience(rawOffset, context, plugin, lastHOffset, lastVOffset);
        data.setPredictionReducedDeviation(reducedOffset);
        double reducedHorizontalDeviation = PredictionUncertaintyHandler.reduceOffsetWithLenience(horizontalDeviation, context, plugin, lastHOffset, lastVOffset);
        data.setPredictionReducedHorizontalDeviation(reducedHorizontalDeviation);
        data.setPredictionBestProfile(bestCandidate == null ? "none" : bestCandidate.getProfile());
        data.markPredictionReady(frame.getTimestampNanos());

        // ── Grim OffsetHandler pattern ──
        // Config mirrors Grim's Simulation section
        // Increased threshold to 0.005 to absorb 1.7.10 MathHelper/Float noise
        double threshold = plugin.getConfig().getDouble("Simulation.threshold", 0.005D);
        double immediateSetbackThreshold = plugin.getConfig().getDouble("Simulation.immediate-setback-threshold", 0.1D);
        double maxAdvantage = plugin.getConfig().getDouble("Simulation.max-advantage", 1.0D);
        double maxCeiling = plugin.getConfig().getDouble("Simulation.max-ceiling", 4.0D);
        double setbackDecayMultiplier = plugin.getConfig().getDouble("Simulation.setback-decay-multiplier", 0.999D);

        // Apply extra tolerance for non-aligned state or lag
        double extraTolerance = 0.0D;
        if (!state.isFullyAligned()) {
            extraTolerance += plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D);
        } else if (isLagging(data)) {
            extraTolerance += plugin.getConfig().getDouble("prediction.lag-small-tolerance", 0.03D);
        }

        // The effective offset after uncertainty reduction and extra tolerance
        double offset = Math.max(0.0D, reducedOffset - extraTolerance);
        String bestProfile = bestCandidate == null ? "none" : bestCandidate.getProfile();
        boolean terrainNoise = context.isRecentUnevenGround() || context.isRecentSnowLayerGround() || context.isNearPartialGround();
        if (terrainNoise) {
            if (deltaY < -0.20D) {
                offset = Math.max(0.0D, offset - 0.08D);
            }
            if (bestProfile.contains("wall-x,ceiling") || bestProfile.contains("wall-z,ceiling")
                    || bestProfile.contains("y=0.33") || bestProfile.contains("y=-0.02")) {
                offset = Math.max(0.0D, offset - 0.025D);
            }
        }

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " [Prediction] rawOffset=" + fmt(rawOffset)
                    + " reduced=" + fmt(reducedOffset)
                    + " offset=" + fmt(offset)
                    + " threshold=" + fmt(threshold)
                    + " advantage=" + fmt(data.getBuffer(ADVANTAGE_KEY))
                    + " maxAdv=" + fmt(maxAdvantage)
                    + " h=" + fmt(horizontal)
                    + " dY=" + fmt(deltaY)
                    + " pending=" + state.getPendingChanges()
                    + " best=" + bestProfile);
        }

        double terrainAlertFloor = terrainNoise ? plugin.getConfig().getDouble("prediction.terrain-alert-floor", 0.075D) : 0.0D;
        if (terrainNoise && offset < terrainAlertFloor) {
            decayAdvantage(data);
            data.removeOffsetLenience();
            return;
        }

        if (offset >= threshold || offset >= immediateSetbackThreshold) {
            // Accumulate advantage
            double advantage = data.addBuffer(ADVANTAGE_KEY, offset);
            advantage = Math.min(advantage, maxCeiling);
            data.setBuffer(ADVANTAGE_KEY, advantage);

            // Grim OffsetHandler: carry offset into next tick as extra tolerance
            data.giveOffsetLenienceNextTick(offset);

            recordEvidence(data, offset, "PREDICTION_MODEL");

            // Format offset like Grim
            String humanOffset;
            if (offset < 0.001D) {
                humanOffset = String.format(Locale.ROOT, "%.4E", offset).replace("E-0", "E-");
            } else {
                humanOffset = String.format(Locale.ROOT, "%6f", offset).replace("0.", ".");
            }

            if (advantage >= maxAdvantage || offset >= immediateSetbackThreshold) {
                // Flag and setback
                String detail = humanOffset
                        + " adv=" + fmt(advantage)
                        + " best=" + bestProfile
                        + " h=" + fmt(horizontal)
                        + " dY=" + fmt(deltaY);
                flag(player, data, offset, detail);
            } else {
                // Alert only (accumulating)
                plugin.alerts().alert(player, getName(), data.getViolation(getName()),
                        humanOffset + " adv=" + fmt(advantage)
                        + " best=" + bestProfile);
            }
        } else {
            // No significant offset decay advantage
            decayAdvantage(data);
        }

        // Grim Offset: remove lenience after processing
        data.removeOffsetLenience();
    }

    private void decayAdvantage(PlayerData data) {
        double decayMultiplier = plugin.getConfig().getDouble("Simulation.setback-decay-multiplier", 0.999D);
        data.scaleBuffer(ADVANTAGE_KEY, decayMultiplier);
        // Linear subtraction: even with vector-level prediction,
        // environmental noise (block boundaries, entity collision, etc.) can produce
        // small residual offsets. Subtract a fixed amount per clean tick
        // to prevent infinite advantage buildup from noise.
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










