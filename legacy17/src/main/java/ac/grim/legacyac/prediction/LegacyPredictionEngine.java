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
    /** Movement threshold for 1.7/1.8 clients values below this are zeroed per axis */
    private static final double MOVEMENT_THRESHOLD = 0.005D;

    public static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float)Math.sin((double)i * Math.PI * 2.0D / 65536.0D);
        }
    }

    public static float mathHelperSin(float value) {
        return SIN_TABLE[(int)(value * 10430.378F) & 65535];
    }

    public static float mathHelperCos(float value) {
        return SIN_TABLE[(int)(value * 10430.378F + 16384.0F) & 65535];
    }

    private LegacyPredictionEngine() {
    }

    public static PredictionResult predictMovement(Player player, Material feetBlock, Material belowBlock,
            double lastDeltaY, double lastDeltaXZ, boolean onGround, PlayerData.PredictionContext context,
            int highFallRecoveryTicks) {
        float yaw = player.getLocation().getYaw();
        List<CandidateVelocity> candidates = generateResolvedCandidates(player, feetBlock, belowBlock,
                lastDeltaY, lastDeltaXZ, onGround, context, highFallRecoveryTicks, yaw);

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
            int highFallRecoveryTicks, double observedHorizontal, double observedVertical, double reducedAllowance, float packetYaw) {
        List<CandidateVelocity> candidates = generateResolvedCandidates(player, feetBlock, belowBlock,
                lastDeltaY, lastDeltaXZ, onGround, context, highFallRecoveryTicks, packetYaw);

        final double obsH = observedHorizontal;
        final double obsV = observedVertical;

        Collections.sort(candidates, new Comparator<CandidateVelocity>() {
            @Override
            public int compare(CandidateVelocity left, CandidateVelocity right) {
                double leftDev = candidateDeviation(left, obsH, obsV);
                double rightDev = candidateDeviation(right, obsH, obsV);
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

    /**
     * Vector-level candidate evaluation  compares actual motionX/motionZ from
     * ProtocolLib packet-level shadow tracking against candidate vectors.
     * This provides Grim-quality precision by eliminating the direction-sampling
     * uncertainty inherent in scalar-only deltaXZ comparison.
     */
    public static PredictionEvaluation evaluateBestCandidateVector(Player player, Material feetBlock, Material belowBlock,
            double lastDeltaY, double lastDeltaXZ, boolean onGround, PlayerData.PredictionContext context,
            int highFallRecoveryTicks, double observedMotionX, double observedMotionZ, double observedMotionY,
            double prevMotionX, double prevMotionZ, double reducedAllowance, float packetYaw) {
        List<CandidateVelocity> candidates = generateResolvedCandidatesVector(player, feetBlock, belowBlock,
                lastDeltaY, lastDeltaXZ, onGround, context, highFallRecoveryTicks, prevMotionX, prevMotionZ, packetYaw);

        final double omx = observedMotionX;
        final double omz = observedMotionZ;
        final double omy = observedMotionY;

        Collections.sort(candidates, new Comparator<CandidateVelocity>() {
            @Override
            public int compare(CandidateVelocity left, CandidateVelocity right) {
                double leftDev = vectorDeviation(left, omx, omz, omy);
                double rightDev = vectorDeviation(right, omx, omz, omy);
                int cmp = Double.compare(leftDev, rightDev);
                return cmp != 0 ? cmp : left.getProfile().compareTo(right.getProfile());
            }
        });

        CandidateVelocity bestCandidate = candidates.isEmpty() ? null : candidates.get(0);
        double rawOffset = bestCandidate != null ? vectorDeviation(bestCandidate, omx, omz, omy) : 0.0D;
        double reducedOffset = Math.max(0.0D, rawOffset - Math.max(0.0D, reducedAllowance));
        return new PredictionEvaluation(candidates, bestCandidate, rawOffset, reducedOffset);
    }

    /** Full 3D vector deviation between observed motion and candidate. */
    private static double vectorDeviation(CandidateVelocity c, double obsX, double obsZ, double obsY) {
        double dx = obsX - c.getMotionX();
        double dz = obsZ - c.getMotionZ();
        double dy = obsY - c.getMotionY();
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }

    // ══════════════════════════════════════════════════════════════════
    // Core candidate generation  Vector level (Actual X/Z tracking)
    // ══════════════════════════════════════════════════════════════════

    public static List<CandidateVelocity> generateResolvedCandidatesVector(Player player, Material feetBlock,
            Material belowBlock, double lastDeltaY, double lastDeltaXZ, boolean onGround,
            PlayerData.PredictionContext context, int highFallRecoveryTicks, double prevMotionX, double prevMotionZ, float yaw) {

        double slipperiness = getBlockSlipperiness(belowBlock);
        double friction = onGround ? slipperiness * 0.91D : 0.91D;

        double attributeSpeed = getBaseMoveSpeed(player);
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

        double carriedMomX = prevMotionX * friction;
        double carriedMomZ = prevMotionZ * friction;

        boolean liquidRestricted = context != null && context.isInLiquid();

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
            verticalCandidates.add(0.0D);
            verticalCandidates.add(-0.0784000015258789D);
            verticalCandidates.add(jumpVel);
            verticalCandidates.add(-0.0784000015258789D * 2);

            // Comprehensive Step heights (Up and Down)
            double[] stepHeights = {
                0.015625D, 0.0625D, 0.125D, 0.1875D, 0.25D, 0.3125D, 0.375D, 0.4375D, 0.5D,
                0.5625D, 0.625D, 0.75D, 0.8125D, 0.875D, 1.0D,
                -0.015625D, -0.0625D, -0.125D, -0.1875D, -0.25D, -0.3125D, -0.375D, -0.4375D, -0.5D,
                -0.5625D, -0.625D, -0.75D, -0.8125D, -0.875D, -1.0D
            };
            for (double step : stepHeights) {
                verticalCandidates.add(step);
                verticalCandidates.add(step - 0.0784000015258789D);
            }
        } else {
            double expectedY = (lastDeltaY - GRAVITY) * Y_DRAG;
            if (onLadder) {
                verticalCandidates.add(-0.15D);
                verticalCandidates.add(0.0D);
                verticalCandidates.add(0.2D);
                verticalCandidates.add(expectedY);
            } else if (inLiquid) {
                double swimUp = 0.04D;
                verticalCandidates.add(expectedY);
                verticalCandidates.add(Math.max(expectedY, -0.02D));
                verticalCandidates.add(swimUp);
                verticalCandidates.add(0.3D);
                verticalCandidates.add(-0.02D);
                if (Math.abs(lastDeltaY) > 0.01D) {
                    verticalCandidates.add(lastDeltaY * 0.8D);
                }
            } else {
                // Air physics
                verticalCandidates.add(expectedY);
                verticalCandidates.add(0.0D);
                verticalCandidates.add(-0.0784000015258789D);

                // Partial landing candidates (landing on different heights)
                double[] potentialLandings = {-0.125D, -0.25D, -0.5D, -0.0625D, -0.015625D, -0.375D};
                for (double l : potentialLandings) {
                    verticalCandidates.add(l);
                    verticalCandidates.add(l - 0.0784D); // Landing + gravity force

                    // Double-buffered landing (1.7 jitter)
                    verticalCandidates.add((l - 0.0784D) * 0.98D);
                }

                // Landing ground snap
                verticalCandidates.add(0.0D);
                verticalCandidates.add(-0.0784D);
            }
        }

        if (context != null && context.isRecentVelocity()) {
            verticalCandidates.add(lastDeltaY);
            verticalCandidates.add(lastDeltaY * 0.96D);
        }
        if (context != null && context.isRecentHighFall()) {
            int recovery = Math.max(6, Math.min(10, highFallRecoveryTicks));
            for (int i = 0; i < recovery; i++) {
                verticalCandidates.add(-0.01D * (i + 1));
            }
            verticalCandidates.add(0.0D);
        }
        if (liquidRestricted) {
            verticalCandidates.add(-0.02D);
            verticalCandidates.add(0.0D);
        }

        if (context != null && (context.isRecentUnevenGround() || context.isRecentSnowLayerGround() || context.isNearPartialGround())) {
            verticalCandidates.add(-0.03125D);
            verticalCandidates.add(-0.046875D);
            verticalCandidates.add(-0.09375D);
            verticalCandidates.add(-0.109375D);
            verticalCandidates.add(0.015625D);
            verticalCandidates.add(0.03125D);
            verticalCandidates.add(0.248136D);
            verticalCandidates.add(0.333200D);
            verticalCandidates.add(0.419999D);
        }

        float f_yaw = yaw * 0.017453292F;
        double sinYaw = mathHelperSin(f_yaw);
        double cosYaw = mathHelperCos(f_yaw);

        float[] inputValues = new float[]{-1.0f, 0.0f, 1.0f};
        List<CandidateVelocity> candidates = new ArrayList<CandidateVelocity>();

        for (float f_in : inputValues) {
            for (float s_in : inputValues) {
                float vanillaForward = f_in * 0.98f;
                float vanillaStrafe = s_in * 0.98f;

                double inputMag = vanillaForward * vanillaForward + vanillaStrafe * vanillaStrafe;
                if (inputMag < 1.0E-4D) {
                    double totalX = carriedMomX;
                    double totalZ = carriedMomZ;
                    if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
                    if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

                    for (Double yCandidate : verticalCandidates) {
                        String profile = "vector:f=0.0,s=0.0,y=" + String.format("%.2f", yCandidate);
                        CandidateVelocity c = new CandidateVelocity(profile, totalX, yCandidate, totalZ);
                        candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));
                    }
                    continue;
                }

                double f_val = Math.sqrt(inputMag);
                if (f_val < 1.0D) {
                    f_val = 1.0D;
                }
                double multiplier = acceleration / f_val;

                if (liquidRestricted) {
                    multiplier *= 0.55D;
                }

                double scaledForward = vanillaForward * multiplier;
                double scaledStrafe = vanillaStrafe * multiplier;

                double accelX = scaledStrafe * cosYaw - scaledForward * sinYaw;
                double accelZ = scaledForward * cosYaw + scaledStrafe * sinYaw;

                double totalX = carriedMomX + accelX;
                double totalZ = carriedMomZ + accelZ;

                if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
                if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

                for (Double yCandidate : verticalCandidates) {
                    String profile = "vector:f=" + f_in + ",s=" + s_in + ",y=" + String.format("%.2f", yCandidate);
                    CandidateVelocity c = new CandidateVelocity(profile, totalX, yCandidate, totalZ);
                    candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));

                    // Wall collision candidates: Handle hitting a wall by zeroing axes
                    candidates.add(new CandidateVelocity(profile + ",wall-x", 0.0D, yCandidate, totalZ));
                    candidates.add(new CandidateVelocity(profile + ",wall-z", totalX, yCandidate, 0.0D));
                    candidates.add(new CandidateVelocity(profile + ",wall-xz", 0.0D, yCandidate, 0.0D));

                    // Ceiling collision: Handle hitting head against a block while moving up
                    if (yCandidate > 0.001D) {
                        candidates.add(new CandidateVelocity(profile + ",ceiling", totalX, 0.0D, totalZ));
                    }

                    // Corner collisions: combinations of hitting a wall while hitting a ceiling or another wall
                    candidates.add(new CandidateVelocity(profile + ",wall-x,ceiling", 0.0D, 0.0D, totalZ));
                    candidates.add(new CandidateVelocity(profile + ",wall-z,ceiling", totalX, 0.0D, 0.0D));
                }
            }
        }

        if (onGround && player.isSprinting()) {
            double sprintJumpX = -(double)(mathHelperSin(f_yaw) * 0.2F);
            double sprintJumpZ = (double)(mathHelperCos(f_yaw) * 0.2F);

            for (float f_in : inputValues) {
                for (float s_in : inputValues) {
                    float vanillaForward = f_in * 0.98f;
                    float vanillaStrafe = s_in * 0.98f;

                    double inputMag = vanillaForward * vanillaForward + vanillaStrafe * vanillaStrafe;
                    if (inputMag < 1.0E-4D) continue;

                    double f_val = Math.sqrt(inputMag);
                    if (f_val < 1.0D) {
                        f_val = 1.0D;
                    }
                    double multiplier = acceleration / f_val;

                    double scaledF = vanillaForward * multiplier;
                    double scaledS = vanillaStrafe * multiplier;

                    double ax = scaledS * cosYaw - scaledF * sinYaw + sprintJumpX;
                    double az = scaledF * cosYaw + scaledS * sinYaw + sprintJumpZ;

                    double totalX = carriedMomX + ax;
                    double totalZ = carriedMomZ + az;

                    if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
                    if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

                    for (Double yCandidate : verticalCandidates) {
                        String profile = "vector-sprint-jump:f=" + f_in + ",s=" + s_in + ",y=" + String.format("%.2f", yCandidate);
                        CandidateVelocity c = new CandidateVelocity(profile, totalX, yCandidate, totalZ);
                        candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));

                        // Wall collision for sprint jumping
                        candidates.add(new CandidateVelocity(profile + ",wall-x", 0.0D, yCandidate, totalZ));
                        candidates.add(new CandidateVelocity(profile + ",wall-z", totalX, yCandidate, 0.0D));

                        // Ceiling collision for sprint jumping
                        if (yCandidate > 0.0019D) {
                            candidates.add(new CandidateVelocity(profile + ",ceiling", totalX, 0.0D, totalZ));
                        }
                    }
                }
            }
        }


        if (context != null && context.isRecentVelocity()) {
            CandidateVelocity inertia = new CandidateVelocity("ctx-velocity-inertia",
                    prevMotionX * 0.91D, lastDeltaY, prevMotionZ * 0.91D);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, inertia, onGround));
            CandidateVelocity carry = new CandidateVelocity("ctx-velocity-carry",
                    carriedMomX, (lastDeltaY - GRAVITY) * Y_DRAG, carriedMomZ);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, carry, onGround));
        }

        if (liquidRestricted) {
            CandidateVelocity liquid = new CandidateVelocity("ctx-liquid-slow",
                    prevMotionX * 0.4D, Math.max(-0.08D, (lastDeltaY - GRAVITY) * Y_DRAG),
                    prevMotionZ * 0.4D);
            candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, liquid, onGround));
        }

        candidates.add(new CandidateVelocity("zero", 0.0D, onGround ? 0.0D : ((lastDeltaY - GRAVITY) * Y_DRAG), 0.0D));

        // Wall and Ceiling collision logic - Harmonized with scalar generator
        List<CandidateVelocity> collisionAware = new ArrayList<CandidateVelocity>();
        for (CandidateVelocity c : candidates) {
            collisionAware.add(c);
            collisionAware.add(new CandidateVelocity(c.getProfile() + ",wall-x", 0.0D, c.getMotionY(), c.getMotionZ()));
            collisionAware.add(new CandidateVelocity(c.getProfile() + ",wall-z", c.getMotionX(), c.getMotionY(), 0.0D));
            collisionAware.add(new CandidateVelocity(c.getProfile() + ",wall-xz", 0.0D, c.getMotionY(), 0.0D));

            if (c.getMotionY() > 0.001D) {
                collisionAware.add(new CandidateVelocity(c.getProfile() + ",ceiling", c.getMotionX(), 0.0D, c.getMotionZ()));
            }
        }

        return collisionAware;
    }

    // ══════════════════════════════════════════════════════════════════
    // Core candidate generation  Scalar level (Fallback for missing X/Z)
    // ══════════════════════════════════════════════════════════════════


    public static List<CandidateVelocity> generateResolvedCandidates(Player player, Material feetBlock,
            Material belowBlock, double lastDeltaY, double lastDeltaXZ, boolean onGround,
            PlayerData.PredictionContext context, int highFallRecoveryTicks, float yaw) {

        // ── Step 1: Block friction ──
        double slipperiness = getBlockSlipperiness(belowBlock);
        double friction = onGround ? slipperiness * 0.91D : 0.91D;

        // ── Step 2: Attribute speed ──
        double attributeSpeed = getBaseMoveSpeed(player);
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

            // Comprehensive Step heights (Up and Down)
            double[] stepHeights = {
                0.015625D, 0.0625D, 0.125D, 0.1875D, 0.25D, 0.3125D, 0.375D, 0.4375D, 0.5D,
                0.5625D, 0.625D, 0.75D, 0.8125D, 0.875D, 1.0D,
                -0.015625D, -0.0625D, -0.125D, -0.1875D, -0.25D, -0.3125D, -0.375D, -0.4375D, -0.5D,
                -0.5625D, -0.625D, -0.75D, -0.8125D, -0.875D, -1.0D
            };
            for (double step : stepHeights) {
                verticalCandidates.add(step);
                verticalCandidates.add(step - 0.0784000015258789D);
            }
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
                verticalCandidates.add(0.0D);
                verticalCandidates.add(-0.0784000015258789D);

                // Add intensive landing candidates to absorb jump-end variance
                // These cover various partial coordinates where a player might clip to ground
                double[] potentialLandings = {
                    -0.125D, -0.25D, -0.5D, -0.0625D, -0.1875D, -0.3125D, -0.375D, -0.4375D, -0.5625D, -0.625D, -0.75D, -0.875D, -1.0D
                };
                for (double l : potentialLandings) {
                    verticalCandidates.add(l);
                    verticalCandidates.add(l - 0.0784D); // Landing + gravity step
                }

                // Explicit "Ground snapped" candidates for precise coordinate alignment
                verticalCandidates.add(-Math.abs(lastDeltaY)); // Partial fall stop
                verticalCandidates.add(-0.015625D); // Slabs/Snow landing
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

        if (context != null && (context.isRecentUnevenGround() || context.isRecentSnowLayerGround() || context.isNearPartialGround())) {
            verticalCandidates.add(-0.03125D);
            verticalCandidates.add(-0.046875D);
            verticalCandidates.add(-0.09375D);
            verticalCandidates.add(-0.109375D);
            verticalCandidates.add(0.015625D);
            verticalCandidates.add(0.03125D);
            verticalCandidates.add(0.248136D);
            verticalCandidates.add(0.333200D);
            verticalCandidates.add(0.419999D);
        }

        // ── Step 6: Generate candidates with yaw-rotated inputs ──
        float f_yaw = yaw * 0.017453292F;
        double sinYaw = mathHelperSin(f_yaw);
        double cosYaw = mathHelperCos(f_yaw);

        // Input combinations: forward [-1, 0, 1] × strafe [-1, 0, 1]
        float[] inputValues = new float[]{-1.0f, 0.0f, 1.0f};
        // Also include the carried momentum from previous tick
        double prevCarriedXZ = lastDeltaXZ * friction;

        // Since we only have scalar lastDeltaXZ (not vector motionX/Z), we must
        // sample multiple possible momentum directions. This is the fundamental
        // limitation vs Grim which tracks exact X/Z components.
        // We sample 8 directions (N/S/E/W + diagonals) plus yaw-aligned.

        List<CandidateVelocity> candidates = new ArrayList<CandidateVelocity>();

        for (float f_in : inputValues) {
            for (float s_in : inputValues) {
                float vanillaForward = f_in * 0.98f;
                float vanillaStrafe = s_in * 0.98f;

                double inputMag = vanillaForward * vanillaForward + vanillaStrafe * vanillaStrafe;
                if (inputMag < 1.0E-4D) {
                    // Zero input  only carried momentum
                    addCandidatesForMotion(candidates, player, feetBlock, belowBlock, onGround,
                            prevCarriedXZ, 0.0D, 0.0D,
                            verticalCandidates, yaw, false, "zero", context, liquidRestricted);
                    continue;
                }

                double f_val = Math.sqrt(inputMag);
                if (f_val < 1.0D) {
                    f_val = 1.0D;
                }
                double multiplier = acceleration / f_val;

                if (liquidRestricted) {
                    multiplier *= 0.55D;
                }

                double scaledForward = vanillaForward * multiplier;
                double scaledStrafe = vanillaStrafe * multiplier;

                // Rotate by yaw (vanilla formula)
                double accelX = scaledStrafe * cosYaw - scaledForward * sinYaw;
                double accelZ = scaledForward * cosYaw + scaledStrafe * sinYaw;

                // We try multiple momentum bases since we don't know exact prev X/Z split
                // Base 1: Full momentum in the movement direction
                addCandidatesForAccel(candidates, player, feetBlock, belowBlock, onGround,
                        prevCarriedXZ, accelX, accelZ, verticalCandidates,
                        yaw, f_in, s_in, context, liquidRestricted);
            }
        }

        // ── Step 7: Sprint-jump candidates ──
        if (onGround && player.isSprinting()) {
            // Vanilla sprint-jump: adds -sin(yaw)*0.2 to X and cos(yaw)*0.2 to Z
            // This is the CRITICAL fix  previous version used inputX * 0.2 which was wrong
            double sprintJumpX = -(double)(mathHelperSin(f_yaw) * 0.2F);
            double sprintJumpZ = (double)(mathHelperCos(f_yaw) * 0.2F);

            for (float f_in : inputValues) {
                for (float s_in : inputValues) {
                    float vanillaForward = f_in * 0.98f;
                    float vanillaStrafe = s_in * 0.98f;

                    double inputMag = vanillaForward * vanillaForward + vanillaStrafe * vanillaStrafe;
                    if (inputMag < 1.0E-4D) continue;

                    double f_val = Math.sqrt(inputMag);
                    if (f_val < 1.0D) {
                        f_val = 1.0D;
                    }
                    double multiplier = acceleration / f_val;

                    double scaledF = vanillaForward * multiplier;
                    double scaledS = vanillaStrafe * multiplier;

                    double ax = scaledS * cosYaw - scaledF * sinYaw + sprintJumpX;
                    double az = scaledF * cosYaw + scaledS * sinYaw + sprintJumpZ;

                    // Sprint jump with various previous momentum directions
                    // Similar 8-direction base sampling for sprint jump
                    double[][] sprintMomDirs = {
                        {sinYaw, cosYaw},       // forward roughly
                        {-sinYaw, -cosYaw},     // backward roughly
                        {cosYaw, -sinYaw},      // strafe right roughly
                        {-cosYaw, sinYaw},      // strafe left roughly
                        {0.0D, 0.0D}            // zero momentum
                    };

                    for (double[] dir : sprintMomDirs) {
                        double momX = prevCarriedXZ * dir[0];
                        double momZ = prevCarriedXZ * dir[1];

                        double totalX = momX + ax;
                        double totalZ = momZ + az;

                        // Apply 0.005 movement threshold
                        if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
                        if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

                        String profile = "sprint-jump:f=" + f_in + ",s=" + s_in + ",mom=" + String.format("%.1f", dir[0]);
                        CandidateVelocity c = new CandidateVelocity(profile, totalX, jumpVel, totalZ);
                        candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, c, onGround));
                    }
                }
            }
        }

        // ── Step 8: Contextual special candidates ──
        if (context != null && context.isRecentVelocity()) {
            // Velocity event: the server set the player's velocity, so momentum can be anything.
            // We must sample many directions since knockback can send the player in any direction.
            double[][] velDirs = {
                {sinYaw, cosYaw}, {-sinYaw, -cosYaw},
                {cosYaw, -sinYaw}, {-cosYaw, sinYaw},
                {(sinYaw + cosYaw) * 0.7071D, (cosYaw - sinYaw) * 0.7071D},
                {(sinYaw - cosYaw) * 0.7071D, (cosYaw + sinYaw) * 0.7071D},
                {(-sinYaw + cosYaw) * 0.7071D, (-cosYaw - sinYaw) * 0.7071D},
                {(-sinYaw - cosYaw) * 0.7071D, (-cosYaw + sinYaw) * 0.7071D}
            };
            for (double[] dir : velDirs) {
                // Pure inertia
                CandidateVelocity inertia = new CandidateVelocity("ctx-velocity-inertia",
                        lastDeltaXZ * 0.91D * dir[0], lastDeltaY, lastDeltaXZ * 0.91D * dir[1]);
                candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, inertia, onGround));
                // Carried velocity with gravity
                CandidateVelocity carry = new CandidateVelocity("ctx-velocity-carry",
                        lastDeltaXZ * friction * dir[0], (lastDeltaY - GRAVITY) * Y_DRAG,
                        lastDeltaXZ * friction * dir[1]);
                candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, carry, onGround));
            }
        }
        if (liquidRestricted) {
            // Liquid candidates also need multi-direction sampling
            double[][] liqDirs = {
                {sinYaw, cosYaw}, {-sinYaw, -cosYaw},
                {cosYaw, -sinYaw}, {-cosYaw, sinYaw},
                {0.0D, 0.0D}
            };
            for (double[] dir : liqDirs) {
                CandidateVelocity liquid = new CandidateVelocity("ctx-liquid-slow",
                        lastDeltaXZ * 0.4D * dir[0], Math.max(-0.08D, (lastDeltaY - GRAVITY) * Y_DRAG),
                        lastDeltaXZ * 0.4D * dir[1]);
                candidates.add(CollisionResolver.resolve(player, feetBlock, belowBlock, liquid, onGround));
            }
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

        // Since we only have scalar lastDeltaXZ, we must sample multiple momentum
        // directions. The player's actual previous motion vector could point anywhere.
        // Sample 8 compass directions + zero to cover all possibilities.
        double[][] momentumDirs = {
            {sinY, cosY},       // forward (yaw-aligned)
            {-sinY, -cosY},     // backward
            {cosY, -sinY},      // strafe right
            {-cosY, sinY},      // strafe left
            // diagonals
            {(sinY + cosY) * 0.7071D, (cosY - sinY) * 0.7071D},   // forward-right
            {(sinY - cosY) * 0.7071D, (cosY + sinY) * 0.7071D},   // forward-left
            {(-sinY + cosY) * 0.7071D, (-cosY - sinY) * 0.7071D}, // back-right
            {(-sinY - cosY) * 0.7071D, (-cosY + sinY) * 0.7071D}, // back-left
            {0.0D, 0.0D}       // zero momentum
        };

        double carriedMag = liquidRestricted ? prevCarriedXZ * 0.55D : prevCarriedXZ;

        for (double[] dir : momentumDirs) {
            double momX = carriedMag * dir[0];
            double momZ = carriedMag * dir[1];

            double totalX = momX + accelX;
            double totalZ = momZ + accelZ;

            // Apply 0.005 movement threshold (vanilla 1.7/1.8)
            if (Math.abs(totalX) < MOVEMENT_THRESHOLD) totalX = 0.0D;
            if (Math.abs(totalZ) < MOVEMENT_THRESHOLD) totalZ = 0.0D;

            for (Double candidateY : yCandidates) {
                double cy = candidateY.doubleValue();
                String profile = "f=" + forward + ",s=" + strafe + ",mom=" + String.format("%.1f", dir[0]) + ",y=" + String.format("%.2f", cy);
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
        // Sample 8 compass directions + zero since we don't know the actual direction
        double[][] directions = {
            {sinY, cosY},       // forward
            {-sinY, -cosY},     // backward
            {cosY, -sinY},      // strafe right
            {-cosY, sinY},      // strafe left
            {(sinY + cosY) * 0.7071D, (cosY - sinY) * 0.7071D},   // forward-right
            {(sinY - cosY) * 0.7071D, (cosY + sinY) * 0.7071D},   // forward-left
            {(-sinY + cosY) * 0.7071D, (-cosY - sinY) * 0.7071D}, // back-right
            {(-sinY - cosY) * 0.7071D, (-cosY + sinY) * 0.7071D}, // back-left
            {0.0D, 0.0D}       // zero
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

                // Wall collisions for zero-move/carried motion
                candidates.add(new CandidateVelocity(profile + ",wall-x", 0.0D, cy, totalZ));
                candidates.add(new CandidateVelocity(profile + ",wall-z", totalX, cy, 0.0D));
            }
        }
    }

    private static double getBaseMoveSpeed(Player player) {
        double base = 0.10000000149011612D;
        try {
            float walkSpeed = player.getWalkSpeed();
            if (walkSpeed > 0.0F) {
                base *= (walkSpeed / 0.2F);
            }
        } catch (Throwable ignored) {
        }
        return Math.max(0.02D, Math.min(0.6D, base));
    }

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



