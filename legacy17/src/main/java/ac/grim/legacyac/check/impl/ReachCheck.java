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

        public boolean isLegal() { return legal; }
        public double getDirectDistance() { return directDistance; }
        public long getBoxTimeOffsetMs() { return boxTimeOffsetMs; }
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, Player victim, PlayerData data) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        double maxReach = plugin.getConfig().getDouble("checks.Reach.max-distance", 3.35D) + getAdaptiveReachBonus(data);
        AttackEvaluation eval = evaluate(attacker, plugin.getPlayerData(victim), maxReach, 400L);
        if (!eval.isLegal()) {
            double add = Math.max(0.2D, eval.getDirectDistance() - maxReach);
            double buffer = slideAndAddScore(data, add, plugin.getConfig().getDouble("checks.Reach.window-weight", 1.0D));
            if (buffer > plugin.getConfig().getDouble("checks.Reach.buffer", 0.2D)) {
                flag(attacker, data, add, "ray-miss dist=" + String.format(Locale.ROOT, "%.3f", eval.getDirectDistance()));
                event.setCancelled(true);
            }
        }
        if (eval.isLegal()) {
            coolDownScore(data);
        }
        data.setLastAttackAt(System.currentTimeMillis());
    }

    public AttackEvaluation onUseEntityAttack(Player attacker, Player target, PlayerData attackerData, long backtrackMillis) {
        if (!isEnabled() || isExempt(attacker, attackerData)) {
            return new AttackEvaluation(true, 0.0D, 0L);
        }
        double maxReach = plugin.getConfig().getDouble("checks.Reach.max-distance", 3.35D) + getAdaptiveReachBonus(attackerData);
        AttackEvaluation eval = evaluate(attacker, plugin.getPlayerData(target), maxReach, backtrackMillis);
        if (!eval.isLegal()) {
            double add = Math.max(0.2D, eval.getDirectDistance() - maxReach);
            double buffer = slideAndAddScore(attackerData, add, plugin.getConfig().getDouble("checks.Reach.window-weight", 1.0D));
            if (buffer > plugin.getConfig().getDouble("checks.Reach.buffer", 0.2D)) {
                flag(attacker, attackerData, add, "packet-ray-miss dist=" + String.format(Locale.ROOT, "%.3f", eval.getDirectDistance()));
            }
        }
        if (eval.isLegal()) {
            coolDownScore(attackerData);
        }
        return eval;
    }

    private double getAdaptiveReachBonus(PlayerData data) {
        if (!isLagging(data)) {
            return 0.0D;
        }
        return plugin.getConfig().getDouble("adaptive-lag.reach-extra-distance", 0.15D);
    }

    private AttackEvaluation evaluate(Player attacker, PlayerData targetData, double maxReach, long backtrackMillis) {
        Location eyeLoc = attacker.getEyeLocation();
        Vector origin = eyeLoc.toVector();
        Vector direction = eyeLoc.getDirection();
        if (direction.lengthSquared() < 1.0E-9D) {
            return new AttackEvaluation(false, 999.0D, -1L);
        }
        direction = direction.normalize();

        double directDistance = 999.0D;
        long hitOffset = -1L;
        long now = System.currentTimeMillis();

        List<HitboxFrame> frames = targetData.getHitboxHistorySnapshot(backtrackMillis);
        for (HitboxFrame frame : frames) {
            double cx = (frame.getMinX() + frame.getMaxX()) * 0.5D;
            double cy = (frame.getMinY() + frame.getMaxY()) * 0.5D;
            double cz = (frame.getMinZ() + frame.getMaxZ()) * 0.5D;
            double dist = origin.distance(new Vector(cx, cy, cz));
            if (dist < directDistance) {
                directDistance = dist;
            }
            if (RayTraceUtil.intersectsAabb(origin, direction, maxReach, frame)) {
                hitOffset = now - frame.getTimestampMillis();
                return new AttackEvaluation(true, dist, hitOffset);
            }
        }
        return new AttackEvaluation(false, directDistance, hitOffset);
    }
}
