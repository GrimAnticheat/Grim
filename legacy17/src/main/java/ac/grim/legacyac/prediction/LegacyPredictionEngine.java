package ac.grim.legacyac.prediction;

import ac.grim.legacyac.data.PlayerData;
import java.util.Collections;
import java.util.Comparator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;

/**
 * Prediction engine replicating Minecraft 1.7.10 client movement physics.
 *
 * Aligned with Grim's PredictionEngine approach:
 * 1. Fetch possible start-tick velocities (carried momentum)
 * 2. Apply movement threshold (0.005 for 1.7/1.8)
 * 3. Apply inputs via yaw-rotated getMovementResultFromInput()
 * 4. Add jumps (with proper sprint-jump direction bonus)
 * 5. Collision resolution
 * 6. End-of-tick: gravity, Y drag 0.98, XZ friction
 *
 * Key fixes vs previous version:
 * - Sprint-jump uses vanilla's -sin(yaw)*0.2 / cos(yaw)*0.2 direction formula
 * - Proper yaw-rotated input vectors instead of axis-aligned
 * - 0.005 movement threshold applied per-axis
 * - End-of-tick physics ordering matches vanilla
 * - Proper candidate generation covering all input combinations
 */
public final class LegacyPredictionEngine {
    private static final double GRAVITY = 0.08D;
    private static final double Y_DRAG = 0.98D;
    /** Movement threshold for 1.7/1.8 clients — values below this are zeroed per axis */
    private static final double MOVEMENT_THRESHOLD = 0.005D;

    private LegacyPredictionEngine() {
    }

    public static PredictionResult predict(Player player, Material feetBlock, Material belowBlock,
            double lastDeltaY, double lastDeltaXZ, boolean onGround, PlayerData.PredictionContext context,
            int highFallRecoveryTicks) {
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

    public static PredictionEvaluation evaluateBestCandidate(Player player, Material feetBlock, Material belowBlock,
            double lastDeltaY, double lastDeltaXZ, boolean onGround, PlayerData.PredictionContext context,
            int highFallRecoveryTicks, double observedHorizontal, double observedVertical, double reducedAllowance) {
        List<CandidateVelocity> candidates = generateResolvedCandidates(player, feetBlock, belowBlock,
                lastDeltaY, lastDeltaXZ, onGround, context, highFallRecoveryTicks);

        Collections.sort(candidates, new Comparator<CandidateVelocity>() {
            @Override
            public int compare(CandidateVelocity left, CandidateVelocity right) {
                double leftDev = candidateDeviation(left, observedHorizontal, observedVertical);
                double rightDev = candidateDeviation(right, observedHorizontal, observedVertical);
                int cmp = Double.compare(leftDev, rightDev);
                return cmp != 0 ? cmp : left.getProfile().compareTo(right.getProfile());
            }
        });

        CandidateVelocity bestCandidate = candidates.isEmpty() ? null : candidates.get(0);
        double rawOffset = bestCandidate != null ? candidateDeviation(bestCandidate, observedHorizontal, observedVertical) : 0.0D;
        double reducedOffset = Math.max(0.0D, rawOffset - Math.max(0.0D, reducedAllowance));
        return new PredictionEvaluation(candidates, bestCandidate, rawOffset, reducedOffset);
    }

    private static double candidateDeviation(CandidateVelocity c, double obsH, double obsY) {
        double dh = obsH - c.getHorizontalMagnitude();
        double dv = obsY - c.getMotionY();
        return Math.sqrt(dh * dh + dv * dv);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Core candidate generation — aligned with Grim's approach
    // ═══════════════════════════════════════════════════════════════════

    public static List<CandidateVelocity> generateResolvedCandidates(Player player, Material feetBlock,
            Material belowBlock, double lastDeltaY, double lastDeltaXZ, boolean onGround,
            PlayerData.PredictionContext context, int highFallRecoveryTicks) {

        // ── Step 1: Block friction ──
        double slipperiness = getBlockSlipperiness(belowBlock);
        double friction = onGround ? slipperiness * 0.91D : 0.91D;

        // ── Step 2: Attribute speed ──
        double attributeSpeed = 0.10000000149011612D;
        if (player.isSprinting()) {
            attributeSpeed *= 1.3D;
        }
        PotionEffect speedEffect = findPotion(player, PotionEffectType.SPEED);
        if (speedEffect != null) {
            attributeSpeed *= (1.0D + (speedEffect.getAmplifier() + 1) * 0.2D);
        }
        PotionEffect slowEffect = findPotion(player, PotionEffectType.SLOW);
        if (slowEffect != null) {
            attributeSpeed *= (1.0D - (slowEffect.getAmplifier() + 1) * 0.15D);
        }

        // ── Step 3: Acceleration ──
        double acceleration;
        if (onGround) {
            double frictionCubed = friction * friction * friction;
            acceleration = attributeSpeed * (0.16277136D / frictionCubed);
        } else {
            double airAccel = 0.02D;
            if (player.isSprinting()) {
                airAccel += 0.005999999865889549D;
            }
            acceleration = airAccel;
        }

        // ── Step 4: Carried momentum (from previous tick) ──
        // In vanilla, the client carries lastVelocity * friction into this tick
        // We model the start velocity from the previous tick's delta
        double carriedX = 0.0D;
        double carriedZ = 0.0D;
        // We don't know the exact X/Z split, so we model carried as magnitude
        // This is applied through the candidate loop

        boolean liquidRestricted = context != null && context.isInLiquid();

        // ── Step 5: Vertical candidates ──
        double jumpVel = 0.42D;
        PotionEffect jumpBoost = findPotion(player, PotionEffectType.JUMP);
        if (jumpBoost != null) {
            jumpVel += (jumpBoost.getAmplifier() + 1) * 0.1D;
        }

        Location loc = player.getLocation();
        Block locBlock = loc.getBlock();
        boolean onLadder = locBlock.getType() == Material.LADDER || locBlock.getType() == Material.VINE;
        boolean inLiquid = isLiquid(feetBlock) || isLiquid(belowBlock);

        List<Double> verticalCandidates = new ArrayList<Double>();

        if (onGround) {
            verticalCandidates.add(0.0D);                    // standing still
            verticalCandidates.add(-0.0784000015258789D);     // gravity pull (not jumping)
            verticalCandidates.add((double) jumpVel);         // jump
            // Walk off edge
            verticalCandidates.add(-0.0784000015258789D * 2); // falling off edge (2nd tick)
        } else {
            double expectedY = (lastDeltaY - GRAVITY) * Y_DRAG;

            if (onLadder) {
                verticalCandidates.add(-0.15D);   // ladder fall speed
                verticalCandidates.add(0.0D);     // ladder hold
                verticalCandidates.add(0.2D);     // ladder climb (vanilla: y = min(0.2, y))
                verticalCandidates.add(expectedY);
            } else if (inLiquid) {
                // Liquid: Y can vary widely with swimming
                double swimUp = 0.04D; // liquid swim-up acceleration per tick
                verticalCandidates.add(expectedY);
                verticalCandidates.add(Math.max(expectedY, -0.02D)); // slow sink
                verticalCandidates.add(swimUp);   // swimming up
                verticalCandidates.add(0.3D);     // swim hop out of water
                verticalCandidates.add(-0.02D);    // treading water
                if (Math.abs(lastDeltaY) > 0.01D) {
                    verticalCandidates.add(lastDeltaY * 0.8D); // liquid momentum carry
                }
            } else {
                // Normal air
                verticalCandidates.add(expectedY);
                // Ground collision — Y becomes 0 when landing
                if (expectedY < 0) {
                    verticalCandidates.add(0.0D);
                    // Step-up: landing on a higher block
                    verticalCandidates.add(-0.0784000015258789D);
                }
            }
        }

        // Extra contextual Y candidates
        if (context != null && context.isRecentVelocity()) {
            verticalCandidates.add(lastDeltaY);           // raw velocity carry
            verticalCandidates.add(lastDeltaY * 0.96D);   // velocity with air resistance
        }
        if (context != null && context.isRecentHighFall()) {
            int recovery = Math.max(6, Math.min(10, highFallRecoveryTicks));
            for (int i = 0; i < recovery; i++) {
                verticalCandidates.add(-0.01D * (i + 1));
            }
            verticalCandidates.add(0.0D); // landing frame
        }
        if (liquidRestricted) {
            verticalCandidates.add(-0.02D);
            verticalCandidates.add(0.0D);
        }

        // ── Step 6: Generate candidates with yaw-rotated inputs ──
        float yaw = player.getLocation().getYaw();
        double yawRad = Math.toRadians(yaw);
        double sinYaw = -Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        // Input combinations: forward [-1, 0, 1] × strafe [-1, 0, 1]
        float[] inputValues = new float[]{-1.0f, 0.0f, 1.0f};
        // Also include the carried momentum from previous tick
        double prevCarriedXZ = lastDeltaXZ * friction;

        List<CandidateVelocity> candidates = new ArrayList<CandidateVelocity>();

        for (float forward : inputValues) {
            for (float strafe : inputValues) {
                // Vanilla getMovementResultFromInput():
                // 1. Normalize input if magnitude > 1
                // 2. Multiply by speed
                // 3. Rotate by yaw
                double inputMag = forward * forward + strafe * strafe;
                if (inputMag < 1.0E-8D) {
                    // Zero input — only carried momentum
                    addCandidatesForMotion(candidates, player, feetBlock, belowBlock, onGround,
                            prevCarriedXZ, 0.0D, 0.0D,
                            verticalCandidates, yaw, false, "zero", context, liquidRestricted);
                    continue;
                }

                if (inputMag > 1.0D) {
                    double invLen = 1.0D / Math.sqrt(inputMag);
                    forward *= invLen;
                    strafe *= invLen;
                }

                // Scale by acceleration and 0.98 input multiplier
                double scaledForward = forward * acceleration * 0.98D;
                double scaledStrafe = strafe * acceleration * 0.98D;

                if (liquidRestricted) {
                    scaledForward *= 0.55D;
                    scaledStrafe *= 0.55D;
                }

                // Rotate by yaw (vanilla formula)
                double accelX = scaledStrafe * cosYaw - scaledForward * sinYaw;
                double accelZ = scaledForward * cosYaw + scaledStrafe * sinYaw;

                // We try multiple momentum bases since we don't know exact prev X/Z split
                // Base 1: Full momentum in the movement direction
                addCandidatesForAccel(candidates, player, feetBlock, belowBlock, onGround,
                        prevCarriedXZ, accelX, accelZ, verticalCandidates,
                        yaw, forward, strafe, context, liquidRestricted);
            }
        }

        // ── Step 7: Sprint-jump candidates ──
        if (onGround && player.isSprinting()) {
            // Vanilla sprint-jump: adds -sin(yaw)*0.2 to X and cos(yaw)*0.2 to Z
            // This is the CRITICAL fix — previous version used inputX * 0.2 which was wrong
            double sprintJumpX = (float)(sinYaw * 0.2D);  // vanilla uses float precision
            double sprintJumpZ = (float)(cosYaw * 0.2D);

            for (float forward : inputValues) {
                for (float strafe : inputValues) {
                    double inputMag = forward * forward + strafe * strafe;
                    if (inputMag < 1.0E-4D) continue;

                    if (inputMag > 1.0D) {
                        double invLen = 1.0D / Math.sqrt(inputMag);
                        forward *= invLen;
                        strafe *= invLen;
                    }

                    double scaledF = forward * acceleration * 0.98D;
                    double scaledS = strafe * acceleration * 0.98D;

                    double ax = scaledS * cosYaw - scaledF * sinYaw + sprintJumpX;
                    double az = scaledF * cosYaw + scaledS * sinYaw + sprintJumpZ;

                    // Sprint jump with various previous momentum factors
                    for (double momentumFactor : new double[]{0.0D, 0.5D, 1.0D}) {
                        // We approximate the X/Z split from prev tick by distributing along yaw
                        double momX = prevCarriedXZ * momentumFactor * sinYaw;
                        double momZ = prevCarriedXZ * momentumFactor * cosYaw;

                        double totalX = momX + ax;
                        double totalZ = momZ + az;

                        // Apply 0.005 movement threshold
                        if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
                        if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

                        String profile = "sprint-jump:f=" + forward + ",s=" + strafe + ",mom=" + momentumFactor;
                        CandidateVelocity c = new CandidateVelocity(profile, totalX, jumpVel, totalZ);
                        candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));
                    }
                }
            }
        }

        // ── Step 8: Contextual special candidates ──
        if (context != null && context.isRecentVelocity()) {
            // Velocity event: the server set the player's velocity, so momentum can be anything
            // Add a candidate with pure inertia
            CandidateVelocity inertia = new CandidateVelocity("ctx-velocity-inertia",
                    lastDeltaXZ * 0.91D * sinYaw, lastDeltaY, lastDeltaXZ * 0.91D * cosYaw);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, inertia, onGround));
            // Also try with zero-input (just carried velocity)
            CandidateVelocity carry = new CandidateVelocity("ctx-velocity-carry",
                    lastDeltaXZ * friction * sinYaw, (lastDeltaY - GRAVITY) * Y_DRAG,
                    lastDeltaXZ * friction * cosYaw);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, carry, onGround));
        }
        if (liquidRestricted) {
            CandidateVelocity liquid = new CandidateVelocity("ctx-liquid-slow",
                    lastDeltaXZ * 0.4D * sinYaw, Math.max(-0.08D, (lastDeltaY - GRAVITY) * Y_DRAG),
                    lastDeltaXZ * 0.4D * cosYaw);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, liquid, onGround));
        }

        // ── Step 9: Zero-movement candidate (always possible) ──
        candidates.add(new CandidateVelocity("zero", 0.0D, onGround ? 0.0D : ((lastDeltaY - GRAVITY) * Y_DRAG), 0.0D));

        return candidates;
    }

    /**
     * Add candidates for a given accel vector with multiple momentum distribution assumptions.
     */
    private static void addCandidatesForAccel(List<CandidateVelocity> candidates, Player player,
            Material feetBlock, Material belowBlock, boolean onGround,
            double prevCarriedXZ, double accelX, double accelZ, List<Double> yCandidates,
            float yaw, float forward, float strafe,
            PlayerData.PredictionContext context, boolean liquidRestricted) {

        double yawRad = Math.toRadians(yaw);
        double sinY = -Math.sin(yawRad);
        double cosY = Math.cos(yawRad);
        double friction = onGround ? getBlockSlipperiness(belowBlock) * 0.91D : 0.91D;

        // Try distributing previous momentum along multiple angles
        // This accounts for the fact that we don't know the exact X/Z split of lastDeltaXZ
        double[] momentumFactors = {0.0D, 0.5D, 1.0D};

        for (double momFactor : momentumFactors) {
            // Distribute prev momentum along yaw direction
            double carriedMag = prevCarriedXZ * momFactor;
            if (liquidRestricted) {
                carriedMag *= 0.55D;
            }
            double momX = carriedMag * sinY;
            double momZ = carriedMag * cosY;

            double totalX = momX + accelX;
            double totalZ = momZ + accelZ;

            // Apply 0.005 movement threshold (vanilla 1.7/1.8)
            if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
            if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

            for (Double candidateY : yCandidates) {
                double cy = candidateY.doubleValue();
                String profile = "f=" + forward + ",s=" + strafe + ",mom=" + momFactor + ",y=" + String.format("%.2f", cy);
                CandidateVelocity c = new CandidateVelocity(profile, totalX, cy, totalZ);
                candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));
            }
        }
    }

    /**
     * Add candidates for zero-input with carried momentum.
     */
    private static void addCandidatesForMotion(List<CandidateVelocity> candidates, Player player,
            Material feetBlock, Material belowBlock, boolean onGround,
            double carriedXZ, double accelX, double accelZ, List<Double> yCandidates,
            float yaw, boolean sprint, String tag,
            PlayerData.PredictionContext context, boolean liquidRestricted) {

        double yawRad = Math.toRadians(yaw);
        double sinY = -Math.sin(yawRad);
        double cosY = Math.cos(yawRad);

        double mag = liquidRestricted ? carriedXZ * 0.55D : carriedXZ;

        // For zero input, just carried momentum with friction already applied
        // Try a few momentum directions
        double[][] directions = {
            {sinY, cosY},   // forward
            {-sinY, -cosY}, // backward
            {cosY, -sinY},  // strafe right
            {-cosY, sinY},  // strafe left
            {0.0D, 0.0D}    // zero
        };

        for (double[] dir : directions) {
            double totalX = mag * dir[0];
            double totalZ = mag * dir[1];

            if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
            if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

            for (Double candidateY : yCandidates) {
                double cy = candidateY.doubleValue();
                String profile = tag + ":dir=" + String.format("%.1f,%.1f", dir[0], dir[1]) + ",y=" + String.format("%.2f", cy);
                CandidateVelocity c = new CandidateVelocity(profile, totalX, cy, totalZ);
                candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Block physics helpers
    // ═══════════════════════════════════════════════════════════════════

    private static double getBlockSlipperiness(Material material) {
        if (material == Material.ICE || material == Material.PACKED_ICE) {
            return 0.98D;
        }
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
