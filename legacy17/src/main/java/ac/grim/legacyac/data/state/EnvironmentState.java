package ac.grim.legacyac.data.state;

import ac.grim.legacyac.util.collision.LegacyBlockBoxResolver;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class EnvironmentState {
    private static final int RECENT_TICK_WINDOW = 25;

    private int recentVelocityTicks;
    private int recentRodPullTicks;
    private int recentTeleportTicks;
    private int recentHighFallTicks;
    private int recentEntityCollisionTicks;
    private int recentUnevenGroundTicks;
    private int recentSnowLayerTicks;
    private int recentPartialGroundTicks;
    private int recentIceTicks;
    private int recentHeadHitTicks;

    private boolean inLiquid;
    private boolean stuckEdge;
    private boolean nearGlitchyBlock;
    private boolean nearZeroThreeBoundary;
    private boolean unevenGround;
    private boolean snowLayerGround;
    private boolean nearPartialGround;
    private boolean iceGround;
    private boolean headHitGround;

    private boolean shadowInitialized;
    private double shadowX, shadowY, shadowZ;
    private double shadowMotionX, shadowMotionY, shadowMotionZ;
    private double prevShadowMotionX, prevShadowMotionY, prevShadowMotionZ;
    private double shadowDeviation;

    private final LinkedList<Double> recentDeltaY = new LinkedList<Double>();

    private double predictionMinDeviation;
    private double predictionReducedDeviation;
    private double predictionHorizontalDeviation;
    private double predictionReducedHorizontalDeviation;
    private String predictionBestProfile = "none";
    private long lastPredictionFrameAtNanos;
    private boolean predictionFrameValid;

    private double lastHorizontalOffset;
    private double lastVerticalOffset;

    private double usingItemConfidence;
    private int ticksUsingItem;
    private int noSlowConsecutiveViolationTicks;
    private int speedLevel;

    public void tick(Player player, Location to, boolean onGround, double deltaXZ, double deltaY,
            boolean teleportPending) {
        recentVelocityTicks = Math.max(0, recentVelocityTicks - 1);
        recentRodPullTicks = Math.max(0, recentRodPullTicks - 1);
        recentTeleportTicks = Math.max(0, recentTeleportTicks - 1);
        recentHighFallTicks = Math.max(0, recentHighFallTicks - 1);
        recentEntityCollisionTicks = Math.max(0, recentEntityCollisionTicks - 1);
        recentUnevenGroundTicks = Math.max(0, recentUnevenGroundTicks - 1);
        recentSnowLayerTicks = Math.max(0, recentSnowLayerTicks - 1);
        recentPartialGroundTicks = Math.max(0, recentPartialGroundTicks - 1);
        recentIceTicks = Math.max(0, recentIceTicks - 1);
        recentHeadHitTicks = Math.max(0, recentHeadHitTicks - 1);

        speedLevel = 0;
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(org.bukkit.potion.PotionEffectType.SPEED)) {
                speedLevel = effect.getAmplifier() + 1;
                break;
            }
        }

        Block feetBlock = to.getBlock();
        Block belowBlock = to.clone().add(0.0D, -1.0D, 0.0D).getBlock();
        Material feet = feetBlock.getType();
        Material below = belowBlock.getType();
        byte feetData = feetBlock.getData();
        byte belowData = belowBlock.getData();

        inLiquid = isLiquid(feet) || isLiquid(below);
        stuckEdge = onGround && deltaXZ < 0.02D && Math.abs(deltaY) < 0.06D && hasAdjacentDrop(to);
        nearGlitchyBlock = isGlitchy(feet) || isGlitchy(below);
        nearZeroThreeBoundary = nearPointThree(deltaXZ) || nearPointThree(Math.abs(deltaY));
        snowLayerGround = isSnowLayer(feet, feetData) || isSnowLayer(below, belowData);
        nearPartialGround = isPartialGround(feet, feetData) || isPartialGround(below, belowData) || hasPartialNeighbor(to);
        unevenGround = nearPartialGround || hasUnevenGroundProfile(to);
        iceGround = isIce(feet) || isIce(below);
        headHitGround = deltaY > 0.18D && hasHeadCollision(to, player.isSneaking());

        if (snowLayerGround) {
            recentSnowLayerTicks = RECENT_TICK_WINDOW;
        }
        if (nearPartialGround) {
            recentPartialGroundTicks = RECENT_TICK_WINDOW;
        }
        if (unevenGround) {
            recentUnevenGroundTicks = RECENT_TICK_WINDOW;
        }
        if (iceGround) {
            recentIceTicks = RECENT_TICK_WINDOW;
        }
        if (headHitGround) {
            recentHeadHitTicks = Math.max(recentHeadHitTicks, 8);
        }

        if (player.getNearbyEntities(0.6D, 0.8D, 0.6D).size() > 0) {
            recentEntityCollisionTicks = RECENT_TICK_WINDOW;
        }

        if (teleportPending) {
            markTeleport();
        }
        if (onGround && player.getFallDistance() > 3.5F) {
            markHighFall();
        }

        recentDeltaY.addLast(Double.valueOf(deltaY));
        while (recentDeltaY.size() > 10) {
            recentDeltaY.removeFirst();
        }
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
            prevShadowMotionX = 0.0D;
            prevShadowMotionY = 0.0D;
            prevShadowMotionZ = 0.0D;
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

        prevShadowMotionX = shadowMotionX;
        prevShadowMotionY = shadowMotionY;
        prevShadowMotionZ = shadowMotionZ;

        shadowMotionX = x - shadowX;
        shadowMotionY = y - shadowY;
        shadowMotionZ = z - shadowZ;
        shadowX = x;
        shadowY = y;
        shadowZ = z;
    }

    public void markVelocity() { recentVelocityTicks = RECENT_TICK_WINDOW; }
    public void markRodPull() { recentRodPullTicks = RECENT_TICK_WINDOW; }
    public void markTeleport() { recentTeleportTicks = RECENT_TICK_WINDOW; }
    public void markHighFall() { recentHighFallTicks = RECENT_TICK_WINDOW; }

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

    public void resetNoSlowViolationStreak() { noSlowConsecutiveViolationTicks = 0; }
    public int incrementNoSlowViolationStreak() { return ++noSlowConsecutiveViolationTicks; }

    public boolean isParabolaAnomalous(double minAvgError, int minSamples) {
        if (recentDeltaY.size() < minSamples) return false;
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
        return compared != 0 && (totalError / compared) >= minAvgError;
    }

    public String getScenarioTag() {
        if (recentRodPullTicks > 0 && recentVelocityTicks > 0) return "rod_double_pull";
        if (recentRodPullTicks > 0) return "rod_pull";
        if (recentTeleportTicks > 0) return "pearl_displacement";
        if (recentHighFallTicks > 0) return "high_fall_landing";
        if (recentHeadHitTicks > 0) return "head_hit";
        if (recentIceTicks > 0) return "ice_surface";
        if (inLiquid && recentVelocityTicks > 0) return "liquid_hit";
        if (inLiquid) return "liquid_movement";
        if (recentSnowLayerTicks > 0) return "snow_layer_ground";
        if (recentPartialGroundTicks > 0) return "partial_ground";
        if (recentUnevenGroundTicks > 0) return "uneven_ground";
        if (nearGlitchyBlock) return "near_glitchy_block";
        if (nearZeroThreeBoundary) return "point_three_boundary";
        if (stuckEdge) return "edge_stuck";
        if (recentVelocityTicks > 0) return "velocity_window";
        return "normal";
    }

    public boolean isRecentVelocity() { return recentVelocityTicks > 0; }
    public boolean isRecentRodPull() { return recentRodPullTicks > 0; }
    public boolean isRecentTeleport() { return recentTeleportTicks > 0; }
    public boolean isRecentHighFall() { return recentHighFallTicks > 0; }
    public boolean isRecentEntityCollision() { return recentEntityCollisionTicks > 0; }
    public boolean isRecentUnevenGround() { return recentUnevenGroundTicks > 0; }
    public boolean isRecentSnowLayerGround() { return recentSnowLayerTicks > 0; }
    public boolean isRecentIceGround() { return recentIceTicks > 0; }
    public boolean isRecentHeadHit() { return recentHeadHitTicks > 0; }
    public boolean isInLiquid() { return inLiquid; }
    public boolean isStuckEdge() { return stuckEdge; }
    public boolean isNearGlitchyBlock() { return nearGlitchyBlock; }
    public boolean isNearZeroThreeBoundary() { return nearZeroThreeBoundary; }
    public boolean isNearPartialGround() { return nearPartialGround || recentPartialGroundTicks > 0; }

    public double getShadowDeviation() { return shadowDeviation; }
    public double getShadowMotionX() { return shadowMotionX; }
    public double getShadowMotionY() { return shadowMotionY; }
    public double getShadowMotionZ() { return shadowMotionZ; }
    public double getPrevShadowMotionX() { return prevShadowMotionX; }
    public double getPrevShadowMotionY() { return prevShadowMotionY; }
    public double getPrevShadowMotionZ() { return prevShadowMotionZ; }
    public boolean isShadowInitialized() { return shadowInitialized; }
    public double getPredictionMinDeviation() { return predictionMinDeviation; }
    public void setPredictionMinDeviation(double val) { predictionMinDeviation = Math.max(0.0D, val); }
    public double getPredictionReducedDeviation() { return predictionReducedDeviation; }
    public void setPredictionReducedDeviation(double val) { predictionReducedDeviation = Math.max(0.0D, val); }
    public double getPredictionHorizontalDeviation() { return predictionHorizontalDeviation; }
    public void setPredictionHorizontalDeviation(double val) { predictionHorizontalDeviation = Math.max(0.0D, val); }
    public double getPredictionReducedHorizontalDeviation() { return predictionReducedHorizontalDeviation; }
    public void setPredictionReducedHorizontalDeviation(double val) { predictionReducedHorizontalDeviation = Math.max(0.0D, val); }
    public String getPredictionBestProfile() { return predictionBestProfile; }
    public void setPredictionBestProfile(String val) { predictionBestProfile = val == null ? "none" : val; }
    public void giveOffsetLenienceNextTick(double offset) {
        double minimized = Math.min(offset, 1.0D);
        lastHorizontalOffset = minimized;
        lastVerticalOffset = minimized;
    }
    public void removeOffsetLenience() { lastHorizontalOffset = 0.0D; lastVerticalOffset = 0.0D; }
    public double getLastHorizontalOffset() { return lastHorizontalOffset; }
    public double getLastVerticalOffset() { return lastVerticalOffset; }
    public double getUsingItemConfidence() { return usingItemConfidence; }
    public int getTicksUsingItem() { return ticksUsingItem; }
    public int getSpeedLevel() { return speedLevel; }

    private static boolean hasAdjacentDrop(Location location) {
        int baseY = location.getBlockY() - 1;
        int baseX = location.getBlockX();
        int baseZ = location.getBlockZ();
        return isAirLike(location.getWorld().getBlockAt(baseX + 1, baseY, baseZ).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX - 1, baseY, baseZ).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ + 1).getType())
                || isAirLike(location.getWorld().getBlockAt(baseX, baseY, baseZ - 1).getType());
    }

    private static boolean hasHeadCollision(Location location, boolean sneaking) {
        double headY = location.getY() + (sneaking ? 1.54D : 1.8D);
        int minX = floor(location.getX() - 0.3D);
        int maxX = floor(location.getX() + 0.3D);
        int minZ = floor(location.getZ() - 0.3D);
        int maxZ = floor(location.getZ() + 0.3D);
        int blockY = floor(headY);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Material material = location.getWorld().getBlockAt(x, blockY, z).getType();
                if (material.isSolid() && !isPartialGround(material, (byte) 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAirLike(Material material) {
        return material == Material.AIR || isLiquid(material);
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private static boolean isIce(Material material) {
        return material == Material.ICE || material == Material.PACKED_ICE;
    }

    private static boolean isSnowLayer(Material material, byte data) {
        return material == Material.SNOW && data >= 0;
    }

    private static boolean isPartialGround(Material material, byte data) {
        if (material == Material.SNOW || material == Material.SOUL_SAND || material == Material.ENCHANTMENT_TABLE
                || material == Material.BED_BLOCK || material == Material.FENCE || material == Material.NETHER_FENCE
                || material == Material.COBBLE_WALL || material == Material.IRON_FENCE) {
            return true;
        }
        String name = material.name();
        return name.contains("STEP") || name.contains("SLAB") || name.contains("STAIRS")
                || name.contains("FENCE") || name.contains("WALL") || "CARPET".equals(name);
    }

    private static boolean hasPartialNeighbor(Location location) {
        int baseY = location.getBlockY() - 1;
        int baseX = location.getBlockX();
        int baseZ = location.getBlockZ();
        return isPartialGround(location.getWorld().getBlockAt(baseX + 1, baseY, baseZ).getType(), location.getWorld().getBlockAt(baseX + 1, baseY, baseZ).getData())
                || isPartialGround(location.getWorld().getBlockAt(baseX - 1, baseY, baseZ).getType(), location.getWorld().getBlockAt(baseX - 1, baseY, baseZ).getData())
                || isPartialGround(location.getWorld().getBlockAt(baseX, baseY, baseZ + 1).getType(), location.getWorld().getBlockAt(baseX, baseY, baseZ + 1).getData())
                || isPartialGround(location.getWorld().getBlockAt(baseX, baseY, baseZ - 1).getType(), location.getWorld().getBlockAt(baseX, baseY, baseZ - 1).getData());
    }

    private static boolean hasUnevenGroundProfile(Location location) {
        int baseY = location.getBlockY() - 1;
        int baseX = location.getBlockX();
        int baseZ = location.getBlockZ();
        double center = getSurfaceHeight(location.getWorld().getBlockAt(baseX, baseY, baseZ));
        double east = getSurfaceHeight(location.getWorld().getBlockAt(baseX + 1, baseY, baseZ));
        double west = getSurfaceHeight(location.getWorld().getBlockAt(baseX - 1, baseY, baseZ));
        double south = getSurfaceHeight(location.getWorld().getBlockAt(baseX, baseY, baseZ + 1));
        double north = getSurfaceHeight(location.getWorld().getBlockAt(baseX, baseY, baseZ - 1));
        return Math.abs(center - east) > 1.0E-4D || Math.abs(center - west) > 1.0E-4D
                || Math.abs(center - south) > 1.0E-4D || Math.abs(center - north) > 1.0E-4D;
    }

    private static double getSurfaceHeight(Block block) {
        if (block == null) {
            return 0.0D;
        }
        return LegacyBlockBoxResolver.getTopHeight(block.getType(), block.getData());
    }

    private static boolean isGlitchy(Material material) {
        return material == Material.LADDER || material == Material.VINE
                || material == Material.WEB || material == Material.SOUL_SAND;
    }

    private static boolean nearPointThree(double value) {
        return Math.abs(value - 0.03D) <= 0.005D || Math.abs(value - 0.06D) <= 0.005D;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
