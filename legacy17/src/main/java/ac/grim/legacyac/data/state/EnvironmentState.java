package ac.grim.legacyac.data.state;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.LinkedList;

/**
 * Domain state aggregate for environmental context.
 * Tracks liquids, ladders, glitchy blocks, entity collisions, shadow physics,
 * and the PredictionContext (merged from PlayerData.PredictionContext).
 */
public final class EnvironmentState {
    private static final int RECENT_TICK_WINDOW = 10;

    // Recent event ticks
    private int recentVelocityTicks;
    private int recentRodPullTicks;
    private int recentTeleportTicks;
    private int recentHighFallTicks;
    private int recentEntityCollisionTicks;

    // Current environment flags
    private boolean inLiquid;
    private boolean stuckEdge;
    private boolean nearGlitchyBlock;
    private boolean nearZeroThreeBoundary;

    // Shadow physics (simple prediction model)
    private boolean shadowInitialized;
    private double shadowX, shadowY, shadowZ;
    private double shadowMotionX, shadowMotionY, shadowMotionZ;
    private double shadowDeviation;

    // Vertical delta history (for parabola checks)
    private final LinkedList<Double> recentDeltaY = new LinkedList<Double>();

    // Prediction context
    private double predictionMinDeviation;
    private double predictionReducedDeviation;
    private double predictionHorizontalDeviation;
    private double predictionReducedHorizontalDeviation;
    private String predictionBestProfile = "none";
    private long lastPredictionFrameAtNanos;
    private boolean predictionFrameValid;

    // Item use tracking
    private double usingItemConfidence;
    private int ticksUsingItem;
    private int noSlowConsecutiveViolationTicks;

    // ── Tick / Update methods ───────────────────────────────────────────

    /**
     * Called every movement tick to update environment detection and countdown
     * timers.
     */
    public void tick(Player player, Location to, boolean onGround, double deltaXZ, double deltaY,
            boolean teleportPending) {
        if (recentVelocityTicks > 0)
            recentVelocityTicks--;
        if (recentRodPullTicks > 0)
            recentRodPullTicks--;
        if (recentTeleportTicks > 0)
            recentTeleportTicks--;
        if (recentHighFallTicks > 0)
            recentHighFallTicks--;
        if (recentEntityCollisionTicks > 0)
            recentEntityCollisionTicks--;

        Material feet = to.getBlock().getType();
        Material below = to.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType();
        inLiquid = isLiquid(feet) || isLiquid(below);
        stuckEdge = onGround && deltaXZ < 0.02D && Math.abs(deltaY) < 0.06D && hasAdjacentDrop(to);
        nearGlitchyBlock = isGlitchy(feet) || isGlitchy(below);
        nearZeroThreeBoundary = nearPointThree(deltaXZ) || nearPointThree(Math.abs(deltaY));

        if (player.getNearbyEntities(0.6D, 0.8D, 0.6D).size() > 0) {
            recentEntityCollisionTicks = RECENT_TICK_WINDOW;
        }

        if (teleportPending)
            markTeleport();
        if (onGround && player.getFallDistance() > 3.5F)
            markHighFall();

        // Update delta Y history
        recentDeltaY.addLast(Double.valueOf(deltaY));
        if (recentDeltaY.size() > 10)
            recentDeltaY.removeFirst();
    }

    public void updateShadowPosition(double x, double y, double z, boolean onGround) {
        if (!shadowInitialized) {
            shadowInitialized = true;
            shadowX = x;
            shadowY = y;
            shadowZ = z;
            shadowMotionX = 0.0D;
            shadowMotionY = 0.0D;
            shadowMotionZ = 0.0D;
            shadowDeviation = 0.0D;
            return;
        }
        double friction = onGround ? (0.91D * 0.60D) : 0.91D;
        double expectedX = shadowX + (shadowMotionX * friction);
        double expectedY = shadowY + ((shadowMotionY - 0.08D) * 0.98D);
        double expectedZ = shadowZ + (shadowMotionZ * friction);

        double diffX = x - expectedX;
        double diffY = y - expectedY;
        double diffZ = z - expectedZ;
        shadowDeviation = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);

        shadowMotionX = x - shadowX;
        shadowMotionY = y - shadowY;
        shadowMotionZ = z - shadowZ;
        shadowX = x;
        shadowY = y;
        shadowZ = z;
    }

    // ── Event markers ──────────────────────────────────────────────────

    public void markVelocity() {
        recentVelocityTicks = RECENT_TICK_WINDOW;
    }

    public void markRodPull() {
        recentRodPullTicks = RECENT_TICK_WINDOW;
    }

    public void markTeleport() {
        recentTeleportTicks = RECENT_TICK_WINDOW;
    }

    public void markHighFall() {
        recentHighFallTicks = RECENT_TICK_WINDOW;
    }

    // ── Prediction frame ───────────────────────────────────────────────

    public void beginPredictionFrame(long frameTimestampNanos) {
        this.lastPredictionFrameAtNanos = frameTimestampNanos;
        this.predictionFrameValid = false;
        this.predictionMinDeviation = 0.0D;
        this.predictionReducedDeviation = 0.0D;
        this.predictionHorizontalDeviation = 0.0D;
        this.predictionReducedHorizontalDeviation = 0.0D;
        this.predictionBestProfile = "none";
    }

    public void markPredictionReady(long frameTimestampNanos) {
        this.lastPredictionFrameAtNanos = frameTimestampNanos;
        this.predictionFrameValid = true;
    }

    public boolean hasPredictionForFrame(long frameTimestampNanos) {
        return predictionFrameValid && lastPredictionFrameAtNanos == frameTimestampNanos;
    }

    // ── Item use tracking ──────────────────────────────────────────────

    public void updateUsingItemSignal(boolean candidateUsingItem) {
        if (candidateUsingItem) {
            usingItemConfidence = Math.min(1.0D, usingItemConfidence + 0.5D);
        } else {
            usingItemConfidence = Math.max(0.0D, usingItemConfidence - 0.4D);
        }
        if (usingItemConfidence >= 0.6D) {
            ticksUsingItem++;
        } else {
            ticksUsingItem = 0;
        }
    }

    public void resetNoSlowViolationStreak() {
        noSlowConsecutiveViolationTicks = 0;
    }

    public int incrementNoSlowViolationStreak() {
        return ++noSlowConsecutiveViolationTicks;
    }

    // ── Parabola check ─────────────────────────────────────────────────

    public boolean isParabolaAnomalous(double minAvgError, int minSamples) {
        if (recentDeltaY.size() < minSamples)
            return false;
        double totalError = 0.0D;
        int compared = 0;
        Double previous = null;
        for (Double current : recentDeltaY) {
            if (previous != null) {
                double expected = (previous.doubleValue() - 0.08D) * 0.98D;
                totalError += Math.abs(current.doubleValue() - expected);
                compared++;
            }
            previous = current;
        }
        if (compared == 0)
            return false;
        return (totalError / compared) >= minAvgError;
    }

    // ── Scenario tag (for debug) ───────────────────────────────────────

    public String getScenarioTag() {
        if (recentRodPullTicks > 0 && recentVelocityTicks > 0)
            return "rod_double_pull";
        if (recentRodPullTicks > 0)
            return "rod_pull";
        if (recentTeleportTicks > 0)
            return "pearl_displacement";
        if (recentHighFallTicks > 0)
            return "high_fall_landing";
        if (inLiquid && recentVelocityTicks > 0)
            return "liquid_hit";
        if (inLiquid)
            return "liquid_movement";
        if (nearGlitchyBlock)
            return "near_glitchy_block";
        if (nearZeroThreeBoundary)
            return "point_three_boundary";
        if (stuckEdge)
            return "edge_stuck";
        if (recentVelocityTicks > 0)
            return "velocity_window";
        return "normal";
    }

    // ── Read interface ──────────────────────────────────────────────────

    public boolean isRecentVelocity() {
        return recentVelocityTicks > 0;
    }

    public boolean isRecentRodPull() {
        return recentRodPullTicks > 0;
    }

    public boolean isRecentTeleport() {
        return recentTeleportTicks > 0;
    }

    public boolean isRecentHighFall() {
        return recentHighFallTicks > 0;
    }

    public boolean isRecentEntityCollision() {
        return recentEntityCollisionTicks > 0;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public boolean isStuckEdge() {
        return stuckEdge;
    }

    public boolean isNearGlitchyBlock() {
        return nearGlitchyBlock;
    }

    public boolean isNearZeroThreeBoundary() {
        return nearZeroThreeBoundary;
    }

    public double getShadowDeviation() {
        return shadowDeviation;
    }

    public double getPredictionMinDeviation() {
        return predictionMinDeviation;
    }

    public void setPredictionMinDeviation(double val) {
        this.predictionMinDeviation = Math.max(0.0D, val);
    }

    public double getPredictionReducedDeviation() {
        return predictionReducedDeviation;
    }

    public void setPredictionReducedDeviation(double val) {
        this.predictionReducedDeviation = Math.max(0.0D, val);
    }

    public double getPredictionHorizontalDeviation() {
        return predictionHorizontalDeviation;
    }

    public void setPredictionHorizontalDeviation(double val) {
        this.predictionHorizontalDeviation = Math.max(0.0D, val);
    }

    public double getPredictionReducedHorizontalDeviation() {
        return predictionReducedHorizontalDeviation;
    }

    public void setPredictionReducedHorizontalDeviation(double val) {
        this.predictionReducedHorizontalDeviation = Math.max(0.0D, val);
    }

    public String getPredictionBestProfile() {
        return predictionBestProfile;
    }

    public void setPredictionBestProfile(String val) {
        this.predictionBestProfile = val == null ? "none" : val;
    }

    public double getUsingItemConfidence() {
        return usingItemConfidence;
    }

    public int getTicksUsingItem() {
        return ticksUsingItem;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static boolean hasAdjacentDrop(Location location) {
        int baseY = location.getBlockY() - 1;
        int baseX = location.getBlockX();
        int baseZ = location.getBlockZ();
        return isAirLike(location.getWorld().getBlockAt(baseX + 1, baseY, baseZ).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX - 1, baseY, baseZ).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ + 1).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ - 1).getType());
    }

    private static boolean isAirLike(Material material) {
        return material == Material.AIR || isLiquid(material);
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private static boolean isGlitchy(Material material) {
        return material == Material.LADDER || material == Material.VINE
                || material == Material.WEB || material == Material.SOUL_SAND;
    }

    private static boolean nearPointThree(double value) {
        return Math.abs(value - 0.03D) <= 0.005D || Math.abs(value - 0.06D) <= 0.005D;
    }
}
