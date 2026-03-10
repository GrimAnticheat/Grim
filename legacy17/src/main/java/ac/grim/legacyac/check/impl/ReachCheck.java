package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.combat.HitboxFrame;
import ac.grim.legacyac.combat.RayTraceUtil;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.evidence.CombatEvidence;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public final class ReachCheck extends Check {
    private static final double MOVEMENT_THRESHOLD = 0.03D;
    private static final String CANCEL_BUFFER_KEY = "Reach.cancelBuffer";
    private final Map<UUID, RecentPacketReach> recentPacketReach = new ConcurrentHashMap<UUID, RecentPacketReach>();

    public ReachCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Reach");
    }

    public enum ReachEvidenceType {
        NONE,
        REACH,
        HITBOX_MISS
    }

    public static final class AttackEvaluation {
        private final boolean legal;
        private final double directDistance;
        private final long boxTimeOffsetMs;
        private final boolean teleportMarkerHit;
        private final boolean enforceableWindow;
        private final ReachEvidenceType evidenceType;

        public AttackEvaluation(boolean legal, double directDistance, long boxTimeOffsetMs, boolean teleportMarkerHit,
                boolean enforceableWindow, ReachEvidenceType evidenceType) {
            this.legal = legal;
            this.directDistance = directDistance;
            this.boxTimeOffsetMs = boxTimeOffsetMs;
            this.teleportMarkerHit = teleportMarkerHit;
            this.enforceableWindow = enforceableWindow;
            this.evidenceType = evidenceType;
        }

        public boolean isLegal() { return legal; }
        public double getDirectDistance() { return directDistance; }
        public long getBoxTimeOffsetMs() { return boxTimeOffsetMs; }
        public boolean isTeleportMarkerHit() { return teleportMarkerHit; }
        public boolean isEnforceableWindow() { return enforceableWindow; }
        public ReachEvidenceType getEvidenceType() { return evidenceType; }
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, Player victim, PlayerData data) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        PlayerData victimData = plugin.getPlayerData(victim);
        if (!isTargetValidForReach(victim, victimData)) {
            coolDownScore(data);
            data.setLastAttackAt(System.currentTimeMillis());
            return;
        }

        RecentPacketReach recentPacket = recentPacketReach.get(attacker.getUniqueId());
        if (recentPacket != null && recentPacket.matches(victim, 175L)) {
            data.setLastAttackAt(System.currentTimeMillis());
            return;
        }

        double baseReach = plugin.getConfig().getDouble("checks.Reach.Ray-Distance", 3.1D);
        long teleportGrace = plugin.getConfig().getLong("combat.reach-teleport-grace-ms", 350L);
        double strafeSyncMargin = plugin.getConfig().getDouble("checks.Reach.strafe-sync-extra-margin", 0.05D);
        boolean recentTeleportOrPearl = System.currentTimeMillis() - victimData.getLastTeleportOrPearlAt() <= teleportGrace;
        double bonus = getAdaptiveReachBonus(data, victimData) + (recentTeleportOrPearl ? strafeSyncMargin : 0.0D);
        double maxReach = baseReach + bonus;
        AttackEvaluation eval = evaluate(attacker, data, victimData, maxReach, 400L, strafeSyncMargin);
        if (!eval.isLegal()) {
            handleViolation(event, attacker, data, eval, maxReach, baseReach, bonus, recentTeleportOrPearl, "event");
        } else {
            coolDownScore(data);
            double cb = Math.max(0.0D, data.getBuffer(CANCEL_BUFFER_KEY) - 0.25D);
            data.setBuffer(CANCEL_BUFFER_KEY, cb);
        }
        recordReachCombatEvidence(attacker, data, victimData, eval, maxReach, "event");
        data.setLastAttackAt(System.currentTimeMillis());
    }

    public AttackEvaluation onUseEntityAttack(Player attacker, Player target, PlayerData attackerData,
            long backtrackMillis) {
        if (!isEnabled() || isExempt(attacker, attackerData)) {
            return new AttackEvaluation(true, 0.0D, 0L, false, true, ReachEvidenceType.NONE);
        }

        PlayerData targetData = plugin.getPlayerData(target);
        if (!isTargetValidForReach(target, targetData)) {
            coolDownScore(attackerData);
            return new AttackEvaluation(true, 0.0D, 0L, false, false, ReachEvidenceType.NONE);
        }

        double baseReach = plugin.getConfig().getDouble("checks.Reach.Ray-Distance", 3.1D);
        long teleportGrace = plugin.getConfig().getLong("combat.reach-teleport-grace-ms", 350L);
        double strafeSyncMargin = plugin.getConfig().getDouble("checks.Reach.strafe-sync-extra-margin", 0.05D);
        boolean recentTeleportOrPearl = System.currentTimeMillis() - targetData.getLastTeleportOrPearlAt() <= teleportGrace;
        double bonus = getAdaptiveReachBonus(attackerData, targetData) + (recentTeleportOrPearl ? strafeSyncMargin : 0.0D);
        double maxReach = baseReach + bonus;
        AttackEvaluation eval = evaluate(attacker, attackerData, targetData, maxReach, backtrackMillis, strafeSyncMargin);
        recentPacketReach.put(attacker.getUniqueId(), new RecentPacketReach(target.getUniqueId(), System.currentTimeMillis()));

        if (!eval.isLegal()) {
            handleViolation(null, attacker, attackerData, eval, maxReach, baseReach, bonus, recentTeleportOrPearl, "packet");
        } else {
            coolDownScore(attackerData);
            double cb = Math.max(0.0D, attackerData.getBuffer(CANCEL_BUFFER_KEY) - 0.25D);
            attackerData.setBuffer(CANCEL_BUFFER_KEY, cb);
        }
        recordReachCombatEvidence(attacker, attackerData, targetData, eval, maxReach, "packet");
        return eval;
    }

    private void handleViolation(EntityDamageByEntityEvent event, Player attacker, PlayerData attackerData,
            AttackEvaluation eval, double maxReach, double baseReach, double bonus,
            boolean recentTeleportOrPearl, String source) {
        if (eval.getEvidenceType() == ReachEvidenceType.HITBOX_MISS) {
            double closeMissGrace = plugin.getConfig().getDouble("checks.Reach.hitbox-miss-close-margin", 0.18D);
            closeMissGrace += Math.min(0.08D, attackerData.getLastDeltaXZ() * 0.10D);
            closeMissGrace += Math.min(0.05D, Math.abs(attackerData.getLastDeltaY()) * 0.08D);
            closeMissGrace += attackerData.getSpeedLevel() > 0 ? 0.03D * attackerData.getSpeedLevel() : 0.0D;
            if (eval.getDirectDistance() <= maxReach + closeMissGrace) {
                coolDownScore(attackerData);
                return;
            }
        }

        String evidencePrefix = eval.getEvidenceType() == ReachEvidenceType.HITBOX_MISS ? "HITBOX_MISS" : "REACH";
        String evidenceTag = evidencePrefix + "_" + source.toUpperCase(Locale.ROOT);
        attackerData.setBuffer(CANCEL_BUFFER_KEY, 1.0D);

        String verbose = eval.getEvidenceType() == ReachEvidenceType.HITBOX_MISS
                ? "type=" + eval.getEvidenceType().name()
                : String.format(Locale.ROOT, "%.5f", eval.getDirectDistance()) + " blocks";

        recordEvidence(attackerData, eval.getDirectDistance() - maxReach, evidenceTag);

        if (!eval.isEnforceableWindow() || recentTeleportOrPearl || eval.isTeleportMarkerHit()) {
            plugin.alerts().alert(attacker, getName(), attackerData.getViolation(getName()),
                    source + "-teleport-grace-only " + verbose + " max=" + String.format(Locale.ROOT, "%.3f", maxReach));
            return;
        }

        String reason = source + "-" + eval.getEvidenceType().name().toLowerCase(Locale.ROOT)
                + " " + verbose + " max="
                + String.format(Locale.ROOT, "%.3f", baseReach)
                + (bonus > 0 ? "+" + String.format(Locale.ROOT, "%.2f", bonus) + "(comp)" : "");
        double add = eval.getEvidenceType() == ReachEvidenceType.HITBOX_MISS
                ? plugin.getConfig().getDouble("checks.Reach.hitbox-miss-add", 0.18D)
                : Math.max(0.15D, eval.getDirectDistance() - maxReach);
        flag(attacker, attackerData, add, reason);
        if (event != null) {
            event.setCancelled(true);
        }
    }

    private boolean isTargetValidForReach(Player target, PlayerData targetData) {
        if (target == null || !target.isOnline()) return false;
        if (target.isDead() || target.getHealth() <= 0.0D) return false;
        PlayerData.MovementStateSnapshot movementSnapshot = targetData.getMovementStateSnapshot();
        if (!movementSnapshot.isTeleportAligned()) return false;
        return !targetData.isTeleportSyncPending();
    }

    private double getAdaptiveReachBonus(PlayerData attackerData, PlayerData targetData) {
        FrameContextSnapshot frameContext = attackerData.getCurrentFrameContext();
        ToleranceBudgetEngine.BudgetSnapshot budget = frameContext != null ? frameContext.getBudgetSnapshot() : getBudget(attackerData);
        if (budget != null) {
            double bonus = budget.getCombatReachMargin();
            bonus += Math.min(0.10D, attackerData.getLastDeltaXZ() * 0.18D);
            if (Math.abs(attackerData.getLastDeltaY()) > 0.25D) bonus += 0.05D;
            if (attackerData.getSpeedLevel() > 0) bonus += 0.03D * attackerData.getSpeedLevel();
            if (targetData != null) {
                bonus += Math.min(0.08D, targetData.getLastDeltaXZ() * 0.15D);
                if (Math.abs(targetData.getLastDeltaY()) > 0.20D) bonus += 0.04D;
                if (targetData.getSpeedLevel() > 0) bonus += 0.02D * targetData.getSpeedLevel();
            }
            return bonus;
        }

        double bonus = 0.0D;
        if (isLagging(attackerData)) {
            bonus += plugin.getConfig().getDouble("adaptive-lag.reach-extra-distance", 0.15D);
        }
        long timeSinceVelocity = System.currentTimeMillis() - attackerData.getLastVelocityAt();
        if (timeSinceVelocity < 600L && attackerData.getLastVelocityXZ() > 0.1D) {
            double decay = 1.0D - (timeSinceVelocity / 600.0D);
            bonus += attackerData.getLastVelocityXZ() * decay * 1.2D;
        }
        bonus += Math.min(0.10D, attackerData.getLastDeltaXZ() * 0.18D);
        if (Math.abs(attackerData.getLastDeltaY()) > 0.25D) bonus += 0.05D;
        if (attackerData.getSpeedLevel() > 0) bonus += 0.03D * attackerData.getSpeedLevel();
        if (targetData != null) {
            bonus += Math.min(0.08D, targetData.getLastDeltaXZ() * 0.15D);
            if (Math.abs(targetData.getLastDeltaY()) > 0.20D) bonus += 0.04D;
            if (targetData.getSpeedLevel() > 0) bonus += 0.02D * targetData.getSpeedLevel();
        }
        return bonus;
    }

    private void recordReachCombatEvidence(Player attacker, PlayerData attackerData, PlayerData targetData,
            AttackEvaluation eval, double maxReach, String source) {
        Location eye = attacker.getEyeLocation();
        FrameContextSnapshot frameContext = attackerData.getCurrentFrameContext();
        CombatEvidence evidence = CombatEvidence.builder(
                CombatEvidence.CombatCheckType.REACH, attacker.getName(),
                targetData != null ? "target" : "unknown")
                .actorPos(eye.getX(), eye.getY(), eye.getZ())
                .actorLook(eye.getYaw(), eye.getPitch())
                .localAttackTime(System.currentTimeMillis())
                .boxTimeOffset(eval.getBoxTimeOffsetMs())
                .directDistance(eval.getDirectDistance())
                .closestHitboxDistance(eval.getDirectDistance())
                .hitboxIntersects(eval.isLegal())
                .teleportMarkerHit(eval.isTeleportMarkerHit())
                .enforceableWindow(eval.isEnforceableWindow())
                .scoring(eval.isLegal() ? 0.0D : Math.max(0.0D, eval.getDirectDistance() - maxReach), maxReach, !eval.isLegal())
                .detail(source + "-" + eval.getEvidenceType().name())
                .frameLink(frameContext == null ? -1L : frameContext.getFrameId(),
                        frameContext == null ? -1 : frameContext.getTxWindowId())
                .build();
        attackerData.recordCombatEvidence(evidence);
    }

    private AttackEvaluation evaluate(Player attacker, PlayerData attackerData, PlayerData targetData, double maxReach,
            long backtrackMillis, double strafeSyncMargin) {
        Location eyeLoc = attacker.getEyeLocation();
        Vector origin = eyeLoc.toVector();
        Vector primaryDir = eyeLoc.getDirection();
        if (primaryDir.lengthSquared() < 1.0E-9D) {
            return new AttackEvaluation(false, 999.0D, -1L, false, false, ReachEvidenceType.REACH);
        }
        primaryDir = primaryDir.normalize();

        float lastYaw = attackerData.getPrevYaw();
        float lastPitch = attackerData.getPrevPitch();
        Vector altDir = getDirection(lastYaw, lastPitch);
        altDir = altDir.lengthSquared() < 1.0E-9D ? primaryDir : altDir.normalize();

        Vector strafeDir = new Vector(primaryDir.getZ(), 0.0D, -primaryDir.getX());
        if (strafeDir.lengthSquared() > 1.0E-9D) {
            strafeDir = strafeDir.normalize();
        }
        Vector syncLeftDir = primaryDir.clone().add(strafeDir.clone().multiply(strafeSyncMargin)).normalize();
        Vector syncRightDir = primaryDir.clone().subtract(strafeDir.clone().multiply(strafeSyncMargin)).normalize();

        double hitboxExpand = resolveHitboxExpand(attackerData);
        double rayLength = maxReach + 3.0D;

        double closestIntersection = Double.MAX_VALUE;
        long hitOffset = -1L;
        boolean markerHit = false;
        boolean enforceableWindow = true;
        long now = System.currentTimeMillis();

        List<HitboxFrame> frames = targetData.getHitboxHistorySnapshot(backtrackMillis);
        for (HitboxFrame frame : frames) {
            if (RayTraceUtil.isVecInside(origin, expandedFrame(frame, hitboxExpand))) {
                return new AttackEvaluation(true, 0.0D, now - frame.getTimestampMillis(), frame.isTeleportMarker(),
                        frame.isEnforceable(), ReachEvidenceType.NONE);
            }

            HitboxFrame expanded = expandedFrame(frame, hitboxExpand);
            double dist = RayTraceUtil.intersectionDistance(origin, primaryDir, rayLength, expanded);
            if (dist < closestIntersection) {
                closestIntersection = dist;
                hitOffset = now - frame.getTimestampMillis();
                markerHit = frame.isTeleportMarker();
                enforceableWindow = frame.isEnforceable();
            }

            double altDist = RayTraceUtil.intersectionDistance(origin, altDir, rayLength, expanded);
            if (altDist < closestIntersection) {
                closestIntersection = altDist;
                hitOffset = now - frame.getTimestampMillis();
                markerHit = frame.isTeleportMarker();
                enforceableWindow = frame.isEnforceable();
            }

            double syncLeftDist = RayTraceUtil.intersectionDistance(origin, syncLeftDir, rayLength, expanded);
            if (syncLeftDist < closestIntersection) {
                closestIntersection = syncLeftDist;
                hitOffset = now - frame.getTimestampMillis();
                markerHit = frame.isTeleportMarker();
                enforceableWindow = frame.isEnforceable();
            }

            double syncRightDist = RayTraceUtil.intersectionDistance(origin, syncRightDir, rayLength, expanded);
            if (syncRightDist < closestIntersection) {
                closestIntersection = syncRightDist;
                hitOffset = now - frame.getTimestampMillis();
                markerHit = frame.isTeleportMarker();
                enforceableWindow = frame.isEnforceable();
            }

            if (!frame.isEnforceable() || !frame.isTransactionAligned()) {
                enforceableWindow = false;
            }
        }

        if (closestIntersection <= maxReach) {
            return new AttackEvaluation(true, closestIntersection, hitOffset, markerHit, enforceableWindow, ReachEvidenceType.NONE);
        }
        if (closestIntersection < Double.MAX_VALUE) {
            return new AttackEvaluation(false, closestIntersection, hitOffset, markerHit, enforceableWindow, ReachEvidenceType.REACH);
        }

        double minReachToBox = Double.MAX_VALUE;
        boolean closestBoxEnforceable = true;
        for (HitboxFrame frame : frames) {
            HitboxFrame expanded = expandedFrame(frame, hitboxExpand);
            double dist = closestPointDistance(origin, expanded);
            if (dist < minReachToBox) {
                minReachToBox = dist;
                markerHit = frame.isTeleportMarker();
                hitOffset = now - frame.getTimestampMillis();
                closestBoxEnforceable = frame.isEnforceable();
            }
        }

        return new AttackEvaluation(false, minReachToBox, hitOffset, markerHit, closestBoxEnforceable, ReachEvidenceType.HITBOX_MISS);
    }

    private double resolveHitboxExpand(PlayerData attackerData) {
        double expand = plugin.getConfig().getDouble("checks.Reach.hitbox-threshold", 0.0005D);
        expand += 0.1D;
        if (attackerData.getLastDeltaXZ() <= MOVEMENT_THRESHOLD && Math.abs(attackerData.getLastDeltaY()) <= MOVEMENT_THRESHOLD) {
            expand += MOVEMENT_THRESHOLD;
        }
        expand += Math.min(0.04D, attackerData.getLastDeltaXZ() * 0.08D);
        if (Math.abs(attackerData.getLastDeltaY()) > 0.25D) {
            expand += 0.03D;
        }
        if (attackerData.getSpeedLevel() > 0) {
            expand += 0.01D * attackerData.getSpeedLevel();
        }
        return expand;
    }

    private static double closestPointDistance(Vector point, HitboxFrame box) {
        double cx = Math.max(box.getMinX(), Math.min(point.getX(), box.getMaxX()));
        double cy = Math.max(box.getMinY(), Math.min(point.getY(), box.getMaxY()));
        double cz = Math.max(box.getMinZ(), Math.min(point.getZ(), box.getMaxZ()));
        double dx = point.getX() - cx;
        double dy = point.getY() - cy;
        double dz = point.getZ() - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static HitboxFrame expandedFrame(HitboxFrame frame, double expand) {
        return new HitboxFrame(frame.getTimestampMillis(), frame.isTeleportMarker(), frame.isTransactionAligned(),
                frame.isEnforceable(), frame.getMinX() - expand, frame.getMinY(), frame.getMinZ() - expand,
                frame.getMaxX() + expand, frame.getMaxY(), frame.getMaxZ() + expand);
    }

    private static Vector getDirection(float yaw, float pitch) {
        double yawRad = Math.toRadians(-yaw - 180.0F);
        double pitchRad = Math.toRadians(-pitch);
        double pitchCos = Math.cos(pitchRad);
        double x = Math.sin(yawRad) * pitchCos;
        double y = Math.sin(pitchRad);
        double z = Math.cos(yawRad) * pitchCos;
        return new Vector(x, y, z);
    }

    private static final class RecentPacketReach {
        private final UUID targetUuid;
        private final long createdAtMillis;

        private RecentPacketReach(UUID targetUuid, long createdAtMillis) {
            this.targetUuid = targetUuid;
            this.createdAtMillis = createdAtMillis;
        }

        private boolean matches(Player target, long maxAgeMillis) {
            return target != null && target.getUniqueId().equals(targetUuid)
                    && System.currentTimeMillis() - createdAtMillis <= maxAgeMillis;
        }
    }
}
