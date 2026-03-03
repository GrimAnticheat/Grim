package ac.grim.legacyac.prediction;

import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;

/**
 * Prediction engine that replicates Minecraft 1.7.10 client movement physics.
 *
 * Vanilla movement flow per tick:
 * 1. Apply last-tick velocity × friction to get "carried" velocity
 * 2. Add acceleration based on input (forward/strafe × speed attribute ×
 * friction factor)
 * 3. Apply 0.005 movement threshold (set axis to 0 if |vel| < 0.005)
 * 4. Perform collision (AABB vs world)
 * 5. End-of-tick: apply gravity, multiply by 0.98 (Y drag), multiply by
 * friction (XZ)
 *
 * Because we don't know the player's inputs (forward/strafe keys), we compute
 * maximum
 * and minimum possible speeds for each axis direction.
 */
public final class LegacyPredictionEngine {
    // Base gravity in vanilla (blocks/tick²)
    private static final double GRAVITY = 0.08D;
    // Y velocity drag multiplier
    private static final double Y_DRAG = 0.98D;

    private LegacyPredictionEngine() {
    }

    /**
     * Predict the maximum allowed horizontal and vertical movement for this tick.
     *
     * @param player      the moving player
     * @param feetBlock   the block at the player's feet position
     * @param belowBlock  the block below the player
     * @param lastDeltaY  the player's Y-axis delta from the previous tick
     * @param lastDeltaXZ the player's XZ-axis delta from the previous tick
     * @param onGround    whether the player was on the ground at the start of this
     *                    tick
     * @return a PredictionResult with maximum horizontal speed, and min/max
     *         vertical range
     */
    public static PredictionResult predict(Player player, Material feetBlock, Material belowBlock,
            double lastDeltaY, double lastDeltaXZ, boolean onGround, PlayerData.PredictionContext context, int highFallRecoveryTicks) {
        List<CandidateVelocity> candidates = generateResolvedCandidates(player, feetBlock, belowBlock,
                lastDeltaY, lastDeltaXZ, onGround, context, highFallRecoveryTicks);

        double maxHorizontal = 0.0D;
        double minVertical = Double.POSITIVE_INFINITY;
        double maxVertical = Double.NEGATIVE_INFINITY;
        for (CandidateVelocity candidate : candidates) {
            double horizontal = candidate.getHorizontalMagnitude();
            if (horizontal > maxHorizontal) {
                maxHorizontal = horizontal;
            }
            if (candidate.getMotionY() < minVertical) {
                minVertical = candidate.getMotionY();
            }
            if (candidate.getMotionY() > maxVertical) {
                maxVertical = candidate.getMotionY();
            }
        }

        maxHorizontal += 0.01D;
        minVertical -= 0.01D;
        maxVertical += 0.01D;

        return new PredictionResult(maxHorizontal, minVertical, maxVertical);
    }

    public static List<CandidateVelocity> generateResolvedCandidates(Player player, Material feetBlock,
            Material belowBlock, double lastDeltaY, double lastDeltaXZ, boolean onGround,
            PlayerData.PredictionContext context, int highFallRecoveryTicks) {
        // ========== HORIZONTAL PREDICTION ==========

        // Step 1: Determine block friction
        // In vanilla, friction is sampled from the block BELOW feet when on ground
        double slipperiness = getBlockSlipperiness(belowBlock);
        double friction;
        if (onGround) {
            friction = slipperiness * 0.91D;
        } else {
            friction = 0.91D;
        }

        // Step 2: Calculate movement "speed" (acceleration factor)
        // On ground: acceleration = 0.1 * (0.1627714 / (friction³))
        // In air: acceleration = 0.02 (fixed air acceleration)
        double acceleration;
        if (onGround) {
            double frictionCubed = friction * friction * friction;
            // Base attribute speed for players at walk speed
            double attributeSpeed = 0.10000000149011612D; // default MOVEMENT_SPEED attribute
            if (player.isSprinting()) {
                attributeSpeed *= 1.3D; // sprint modifier
            }
            // Speed potion effect
            PotionEffect speedEffect = findPotion(player, PotionEffectType.SPEED);
            if (speedEffect != null) {
                attributeSpeed *= (1.0D + (speedEffect.getAmplifier() + 1) * 0.2D);
            }
            PotionEffect slowEffect = findPotion(player, PotionEffectType.SLOW);
            if (slowEffect != null) {
                attributeSpeed *= (1.0D - (slowEffect.getAmplifier() + 1) * 0.15D);
            }
            acceleration = attributeSpeed * (0.16277136D / frictionCubed);
        } else {
            double airAccel = 0.02D;
            if (player.isSprinting()) {
                airAccel += 0.005999999865889549D; // sprint air bonus
            }
            acceleration = airAccel;
        }

        double carriedHorizontal = lastDeltaXZ * friction;
        double inputAccel = acceleration * 0.98D;
        boolean liquidRestricted = context != null && context.isInLiquid();
        if (liquidRestricted) {
            carriedHorizontal *= 0.55D;
            inputAccel *= 0.55D;
        }

        // ========== VERTICAL PREDICTION ==========

        // Vanilla Y physics:
        // newVelY = (oldVelY - gravity) * 0.98
        // But player might jump, take knockback, etc.

        double baselineY;
        double jumpY;

        if (onGround) {
            // Player can stand still (deltaY ≈ 0), walk off an edge (deltaY goes negative),
            // or jump.
            // Normal jump: velY = 0.42 + jumpBoost * 0.1
            double jumpVel = 0.42D;
            PotionEffect jumpBoost = findPotion(player, PotionEffectType.JUMP);
            if (jumpBoost != null) {
                jumpVel += (jumpBoost.getAmplifier() + 1) * 0.1D;
            }

            baselineY = -0.0784000015258789D;
            jumpY = jumpVel;

            // Slime blocks were added in 1.8 — check by name for compatibility
            if (belowBlock.name().equals("SLIME_BLOCK")) {
                jumpY = Math.max(jumpY, Math.abs(lastDeltaY));
            }
        } else {
            // In-air Y prediction
            double expectedY = (lastDeltaY - GRAVITY) * Y_DRAG;
            double tolerance = 0.07D;

            // Extra tolerance for high velocities (knockback, etc.)
            if (Math.abs(lastDeltaY) > 0.5D) {
                tolerance += 0.04D;
            }

            // Ladder/vine climbing
            Location loc = player.getLocation();
            Block locBlock = loc.getBlock();
            if (locBlock.getType() == Material.LADDER || locBlock.getType() == Material.VINE) {
                baselineY = -0.16D;
                jumpY = 0.22D;
            } else if (isLiquid(feetBlock) || isLiquid(belowBlock)) {
                baselineY = Math.min(expectedY - tolerance, -0.08D);
                jumpY = Math.max(expectedY + tolerance, 0.5D);
            } else {
                baselineY = expectedY;
                jumpY = expectedY + tolerance;

                // Collision with ground: Y can be 0 or very small
                if (expectedY < 0) {
                    baselineY = Math.max(expectedY, -0.5D);
                    jumpY = Math.max(jumpY, 0.0D);
                }
            }
        }

        List<CandidateVelocity> candidates = new ArrayList<CandidateVelocity>();
        double[] inputFactors = new double[] { 0.0D, 0.5D, -0.5D, 1.0D, -1.0D };
        double[] verticalCandidates = onGround ? new double[] { 0.0D, baselineY, jumpY }
                : new double[] { baselineY, jumpY, baselineY - 0.05D };
        List<Double> extraVerticalCandidates = new ArrayList<Double>();
        if (context != null && context.isRecentVelocity()) {
            extraVerticalCandidates.add(Double.valueOf(lastDeltaY * 0.96D));
        }
        if (liquidRestricted) {
            extraVerticalCandidates.add(Double.valueOf(Math.max(-0.08D, baselineY * 0.85D)));
        }
        if (context != null && context.isRecentHighFall()) {
            int recovery = Math.max(6, Math.min(10, highFallRecoveryTicks));
            for (int i = 0; i < recovery; i++) {
                extraVerticalCandidates.add(Double.valueOf(-0.01D * (i + 1)));
            }
        }
        for (double inputX : inputFactors) {
            for (double inputZ : inputFactors) {
                if (Math.abs(inputX) == 1.0D && Math.abs(inputZ) == 1.0D) {
                    inputX *= 0.7071067811865476D;
                    inputZ *= 0.7071067811865476D;
                }

                double motionX = (carriedHorizontal * inputX) + (inputAccel * inputX);
                double motionZ = (carriedHorizontal * inputZ) + (inputAccel * inputZ);

                if (onGround && player.isSprinting()) {
                    motionX += 0.2D * inputX;
                    motionZ += 0.2D * inputZ;
                }

                if (isIce(belowBlock) || isIce(feetBlock)) {
                    motionX += 0.02D * inputX;
                    motionZ += 0.02D * inputZ;
                }

                for (double candidateY : verticalCandidates) {
                    String profile = "ix=" + inputX + ",iz=" + inputZ + ",y=" + candidateY;
                    CandidateVelocity candidate = new CandidateVelocity(profile, motionX, candidateY, motionZ);
                    candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, candidate, onGround));
                }
                for (Double extraY : extraVerticalCandidates) {
                    String profile = "ctx-low-weight:ix=" + inputX + ",iz=" + inputZ + ",y=" + extraY.doubleValue();
                    CandidateVelocity candidate = new CandidateVelocity(profile, motionX, extraY.doubleValue(), motionZ);
                    candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, candidate, onGround));
                }
            }
        }
        if (context != null && context.isRecentVelocity()) {
            CandidateVelocity inertia = new CandidateVelocity("ctx-hit-inertia", lastDeltaXZ * 0.91D, lastDeltaY, 0.0D);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, inertia, onGround));
        }
        if (liquidRestricted) {
            CandidateVelocity liquid = new CandidateVelocity("ctx-liquid-restricted", lastDeltaXZ * 0.4D, Math.max(-0.08D, baselineY), 0.0D);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, liquid, onGround));
        }
        return candidates;
    }

    /**
     * Get the block slipperiness (1.7.10 values).
     * Default is 0.6, ice is 0.98, packed ice is 0.98, soul sand is kind of handled
     * separately.
     */
    private static double getBlockSlipperiness(Material material) {
        if (material == Material.ICE || material == Material.PACKED_ICE) {
            return 0.98D;
        }
        // Soul sand doesn't actually change slipperiness in 1.7, it uses a bounding box
        // trick
        // But we model it as lower friction for simplicity
        return 0.6D;
    }

    private static boolean isIce(Material material) {
        return material == Material.ICE || material == Material.PACKED_ICE;
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private static PotionEffect findPotion(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }
}
