package ac.grim.legacyac.prediction;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
    // Minecraft movement threshold for 1.7/1.8
    private static final double MOVEMENT_THRESHOLD = 0.005D;
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
            double lastDeltaY, double lastDeltaXZ, boolean onGround) {
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

        // Step 3: Compute max possible horizontal speed this tick
        // Previous horizontal velocity is carried forward: oldHorizontalVel * friction
        // Then input acceleration is added: up to sqrt(2) * acceleration for diagonal
        // But the actual carried velocity's direction is unknown, so we compute the
        // max.
        //
        // maxHorizontal = lastDeltaXZ * friction + acceleration * sqrt(2)
        // The sqrt(2) factor accounts for diagonal input (both forward and strafe
        // pressed)
        // But normalized input is capped at length 1.0 before scaling by 0.98
        // So actual max input multiplier is min(1.0, sqrt(2)) * 0.98 = 0.98
        double carriedHorizontal = lastDeltaXZ * friction;
        double maxInputAccel = acceleration * 0.98D; // 0.98 is the input scale
        double predictedMaxHorizontal = carriedHorizontal + maxInputAccel;

        // Extra tolerances for edge cases:
        // Jump sprint boost: +0.2 * speed component when sprint-jumping
        if (onGround && player.isSprinting()) {
            // Sprint jump gives a horizontal boost of ~0.2 * baseSpeed direction
            predictedMaxHorizontal += 0.2D;
        }

        // Ice and slime blocks have higher friction, so velocity carries more
        if (isIce(belowBlock) || isIce(feetBlock)) {
            // Already handled by slipperiness, but add tolerance for multi-tick
            // accumulation
            predictedMaxHorizontal += 0.04D;
        }

        // Soul sand slows movement
        if (feetBlock == Material.SOUL_SAND) {
            predictedMaxHorizontal *= 0.4D;
        }

        // Web drastically reduces speed
        if (feetBlock == Material.WEB || belowBlock == Material.WEB) {
            predictedMaxHorizontal = Math.min(predictedMaxHorizontal, 0.05D);
        }

        // Liquid movement
        if (isLiquid(feetBlock) || isLiquid(belowBlock)) {
            // Water/lava use different movement model, allow more tolerance
            predictedMaxHorizontal = Math.max(predictedMaxHorizontal, 0.14D);
        }

        // Movement threshold: if below 0.005, it's zeroed → add tolerance
        predictedMaxHorizontal += MOVEMENT_THRESHOLD;

        // General tolerance for network jitter, collision sliding, etc.
        predictedMaxHorizontal += 0.01D;

        // ========== VERTICAL PREDICTION ==========

        // Vanilla Y physics:
        // newVelY = (oldVelY - gravity) * 0.98
        // But player might jump, take knockback, etc.

        double predictedMinY;
        double predictedMaxY;

        if (onGround) {
            // Player can stand still (deltaY ≈ 0), walk off an edge (deltaY goes negative),
            // or jump.
            // Normal jump: velY = 0.42 + jumpBoost * 0.1
            double jumpVel = 0.42D;
            PotionEffect jumpBoost = findPotion(player, PotionEffectType.JUMP);
            if (jumpBoost != null) {
                jumpVel += (jumpBoost.getAmplifier() + 1) * 0.1D;
            }

            predictedMinY = -0.0784000015258789D; // one tick of gravity from standstill: (0 - 0.08) * 0.98
            predictedMaxY = jumpVel + 0.1D; // jump velocity + tolerance

            // Slime blocks were added in 1.8 — check by name for compatibility
            if (belowBlock.name().equals("SLIME_BLOCK")) {
                predictedMaxY = Math.max(predictedMaxY, Math.abs(lastDeltaY) + 0.1D);
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
                // Climbing: velocity clamped to [-0.15, 0.2]
                predictedMinY = -0.16D;
                predictedMaxY = 0.22D;
            } else if (isLiquid(feetBlock) || isLiquid(belowBlock)) {
                // In liquid: different physics
                predictedMinY = Math.min(expectedY - tolerance, -0.08D);
                predictedMaxY = Math.max(expectedY + tolerance, 0.5D);
            } else {
                predictedMinY = expectedY - tolerance;
                predictedMaxY = expectedY + tolerance;

                // Collision with ground: Y can be 0 or very small
                if (expectedY < 0) {
                    predictedMinY = Math.min(predictedMinY, -0.5D);
                    // Could land on ground and reset
                    predictedMaxY = Math.max(predictedMaxY, 0.0D);
                }
            }
        }

        return new PredictionResult(predictedMaxHorizontal, predictedMinY, predictedMaxY);
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
