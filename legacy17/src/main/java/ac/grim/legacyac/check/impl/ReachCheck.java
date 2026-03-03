package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.combat.HitboxFrame;
import ac.grim.legacyac.combat.RayTraceUtil;
import ac.grim.legacyac.data.PlayerData;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import java.util.Locale;

public final class ReachCheck extends Check {
    public ReachCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Reach");
    }

    public static final class AttackEvaluation {
        private final boolean legal;
        private final double directDistance;
        private final long boxTimeOffsetMs;

        public AttackEvaluation(boolean legal, double directDistance, long boxTimeOffsetMs) {
            this.legal = legal;
            this.directDistance = directDistance;
            this.boxTimeOffsetMs = boxTimeOffsetMs;
        }

        public boolean isLegal() {
            return legal;
        }

        public double getDirectDistance() {
            return directDistance;
        }

        public long getBoxTimeOffsetMs() {
            return boxTimeOffsetMs;
        }
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, Player victim, PlayerData data) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        double baseReach = plugin.getConfig().getDouble("checks.Reach.Ray-Distance", 3.1D);
        double bonus = getAdaptiveReachBonus(data);
        double maxReach = baseReach + bonus;
        AttackEvaluation eval = evaluate(attacker, plugin.getPlayerData(victim), maxReach, 400L);
        if (!eval.isLegal()) {
            double add = Math.max(0.15D, eval.getDirectDistance() - maxReach);
            recordEvidence(data, add, "REACH_EVENT");
            double buffer = slideAndAddScore(data, add,
                    plugin.getConfig().getDouble("checks.Reach.window-weight", 1.0D));
            if (buffer > plugin.getConfig().getDouble("checks.Reach.buffer", 0.5D)) {
                flag(attacker, data, add, "ray-dist=" + String.format(Locale.ROOT, "%.3f", eval.getDirectDistance())
                        + " max=" + String.format(Locale.ROOT, "%.3f", baseReach)
                        + (bonus > 0 ? "+" + String.format(Locale.ROOT, "%.2f", bonus) + "(lag)" : ""));
                event.setCancelled(true);
            }
        }
        if (eval.isLegal()) {
            coolDownScore(data);
        }
        data.setLastAttackAt(System.currentTimeMillis());
    }

    public AttackEvaluation onUseEntityAttack(Player attacker, Player target, PlayerData attackerData,
            long backtrackMillis) {
        if (!isEnabled() || isExempt(attacker, attackerData)) {
            return new AttackEvaluation(true, 0.0D, 0L);
        }
        double baseReach = plugin.getConfig().getDouble("checks.Reach.Ray-Distance", 3.1D);
        double bonus = getAdaptiveReachBonus(attackerData);
        double maxReach = baseReach + bonus;
        AttackEvaluation eval = evaluate(attacker, plugin.getPlayerData(target), maxReach, backtrackMillis);
        if (!eval.isLegal()) {
            double add = Math.max(0.15D, eval.getDirectDistance() - maxReach);
            recordEvidence(attackerData, add, "REACH_PACKET");
            double buffer = slideAndAddScore(attackerData, add,
                    plugin.getConfig().getDouble("checks.Reach.window-weight", 1.0D));
            if (buffer > plugin.getConfig().getDouble("checks.Reach.buffer", 0.5D)) {
                flag(attacker, attackerData, add,
                        "pkt-ray-dist=" + String.format(Locale.ROOT, "%.3f", eval.getDirectDistance()) + " max="
                                + String.format(Locale.ROOT, "%.3f", baseReach)
                                + (bonus > 0 ? "+" + String.format(Locale.ROOT, "%.2f", bonus) + "(lag)" : ""));
            }
        }
        if (eval.isLegal()) {
            coolDownScore(attackerData);
        }
        return eval;
    }

    private double getAdaptiveReachBonus(PlayerData data) {
        double bonus = 0.0D;
        if (isLagging(data)) {
            bonus += plugin.getConfig().getDouble("adaptive-lag.reach-extra-distance", 0.15D);
        }

        // Knockback compensation: after receiving velocity, the server-side position
        // is ahead of where the client thinks they are. This causes inflated ray
        // distances for attacks sent right after being knocked back.
        // Scale bonus by velocity magnitude with time decay.
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 600L && data.getLastVelocityXZ() > 0.1D) {
            double decay = 1.0D - (timeSinceVelocity / 600.0D);
            bonus += data.getLastVelocityXZ() * decay * 1.2D;
        }

        return bonus;
    }

    /**
     * Evaluates whether an attack is within legal reach distance.
     *
     * Key improvements over the previous implementation:
     * 1. Uses ray-surface intersection distance (not center distance) — matches
     * vanilla and Grim
     * 2. Tries multiple look directions (current yaw + last yaw) to handle 1-tick
     * timing differences
     * 3. Properly accounts for the 1.7/1.8 hitbox expansion (+0.1)
     * 4. Checks if the player's eye is inside the target hitbox (distance=0)
     */
    private AttackEvaluation evaluate(Player attacker, PlayerData targetData, double maxReach, long backtrackMillis) {
        Location eyeLoc = attacker.getEyeLocation();
        Vector origin = eyeLoc.toVector();
        Vector primaryDir = eyeLoc.getDirection();
        if (primaryDir.lengthSquared() < 1.0E-9D) {
            return new AttackEvaluation(false, 999.0D, -1L);
        }
        primaryDir = primaryDir.normalize();

        // Also try with slightly different yaw (1-tick behind) — mirrors Grim's
        // multi-look approach
        PlayerData attackerData = plugin.getPlayerData(attacker);
        float lastYaw = attackerData.getPrevYaw();
        float lastPitch = attackerData.getPrevPitch();
        Vector altDir = getDirection(lastYaw, lastPitch);
        if (altDir.lengthSquared() < 1.0E-9D) {
            altDir = primaryDir;
        } else {
            altDir = altDir.normalize();
        }

        // The maximum ray length for intersection checking should be generous
        // to detect "missed hitbox" (Grim uses maxReach + 3)
        double rayLength = maxReach + 3.0D;

        double closestIntersection = Double.MAX_VALUE;
        long hitOffset = -1L;
        long now = System.currentTimeMillis();

        List<HitboxFrame> frames = targetData.getHitboxHistorySnapshot(backtrackMillis);
        for (HitboxFrame frame : frames) {
            // Check if the player's eye is inside the hitbox
            if (RayTraceUtil.isVecInside(origin, frame)) {
                return new AttackEvaluation(true, 0.0D, now - frame.getTimestampMillis());
            }

            // Try primary direction
            double dist = RayTraceUtil.intersectionDistance(origin, primaryDir, rayLength, frame);
            if (dist < closestIntersection) {
                closestIntersection = dist;
                hitOffset = now - frame.getTimestampMillis();
            }

            // Try alternate direction (last tick's look)
            double altDist = RayTraceUtil.intersectionDistance(origin, altDir, rayLength, frame);
            if (altDist < closestIntersection) {
                closestIntersection = altDist;
                hitOffset = now - frame.getTimestampMillis();
            }
        }

        // If we found an intersection within reach, it's legal
        if (closestIntersection <= maxReach) {
            return new AttackEvaluation(true, closestIntersection, hitOffset);
        }

        // If we found an intersection at all (just beyond reach), report the distance
        if (closestIntersection < Double.MAX_VALUE) {
            return new AttackEvaluation(false, closestIntersection, hitOffset);
        }

        // No intersection found at all → hitbox miss
        // This can happen legitimately during jumping/fast movement when stored
        // hitbox frames don't accurately cover the target's actual position.
        // Fall back to closest center distance — if it's within reach, treat as legal.
        double closestCenter = Double.MAX_VALUE;
        for (HitboxFrame frame : frames) {
            double cx = (frame.getMinX() + frame.getMaxX()) * 0.5D;
            double cy = (frame.getMinY() + frame.getMaxY()) * 0.5D;
            double cz = (frame.getMinZ() + frame.getMaxZ()) * 0.5D;
            double dist = origin.distance(new Vector(cx, cy, cz));
            if (dist < closestCenter) {
                closestCenter = dist;
            }
        }

        // If center distance is within reach, the attack is likely legitimate
        // but hitbox timing just didn't line up — don't flag
        if (closestCenter <= maxReach + 0.5D) {
            return new AttackEvaluation(true, closestCenter, hitOffset);
        }

        return new AttackEvaluation(false, closestCenter, hitOffset);
    }

    /**
     * Calculate look direction from yaw and pitch (vanilla 1.7 formula).
     */
    private static Vector getDirection(float yaw, float pitch) {
        double yawRad = Math.toRadians(-yaw - 180.0F);
        double pitchRad = Math.toRadians(-pitch);
        double pitchCos = Math.cos(pitchRad);
        double x = Math.sin(yawRad) * pitchCos;
        double y = Math.sin(pitchRad);
        double z = Math.cos(yawRad) * pitchCos;
        return new Vector(x, y, z);
    }
}
