package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.FrameContextSnapshot;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.evidence.CombatEvidence;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class KillAuraCheck extends Check {
    private final Map<UUID, RotationPatternState> rotationPatterns = new ConcurrentHashMap<UUID, RotationPatternState>();

    public KillAuraCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "KillAura");
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, PlayerData data) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        checkAngles(attacker, data, false);
    }

    public void onUseEntityAttack(Player attacker, Player target, PlayerData data, ReachCheck.AttackEvaluation reachEval) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        boolean suspicious = false;
        boolean severe = reachEval != null && !reachEval.isLegal();
        suspicious |= checkAngles(attacker, data, severe);
        suspicious |= checkRepeatedRotationPattern(attacker, data);
        suspicious |= checkMultiTarget(attacker, data, target);
        suspicious |= checkLineOfSight(attacker, target, data, reachEval);
        suspicious |= checkReachContext(attacker, data, reachEval);

        if (!suspicious) {
            coolDownScore(data);
        }

        data.setLastAttackAt(System.currentTimeMillis());
    }

    private boolean checkAngles(Player attacker, PlayerData data, boolean severe) {
        float yawDelta = Math.abs(data.getLastYawDelta());
        float pitchDelta = Math.abs(data.getLastPitchDelta());
        double horizontal = data.getLastDeltaXZ();
        float snapThreshold = (float) plugin.getConfig().getDouble("checks.KillAura.snap-threshold", 55.0D);

        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        ToleranceBudgetEngine.BudgetSnapshot budget = frameContext != null ? frameContext.getBudgetSnapshot() : getBudget(data);
        if (budget != null && budget.getCombatReachMargin() > 0.1D) {
            snapThreshold += 5.0F;
        }
        if (severe) {
            snapThreshold = Math.max(40.0F, snapThreshold - 10.0F);
        }

        float maxPitchDelta = severe ? 3.0F : 2.0F;
        double maxHorizontal = severe ? 0.16D : 0.08D;
        if (yawDelta <= snapThreshold || pitchDelta >= maxPitchDelta || horizontal >= maxHorizontal) {
            return false;
        }

        double add = severe ? 1.4D : 1.0D;
        double buffer = slideAndAddScore(data, add, 1.0D);
        String detail = "SNAP yaw=" + fmt(yawDelta) + " pitch=" + fmt(pitchDelta);
        recordEvidence(data, add, "KILLAURA_SNAP");
        recordKillAuraCombatEvidence(attacker, data, add, detail);

        if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
            flag(attacker, data, add, detail);
        }
        return true;
    }

    private boolean checkRepeatedRotationPattern(Player attacker, PlayerData data) {
        RotationPatternState state = rotationPatterns.get(attacker.getUniqueId());
        if (state == null) {
            state = new RotationPatternState();
            RotationPatternState existing = rotationPatterns.putIfAbsent(attacker.getUniqueId(), state);
            if (existing != null) {
                state = existing;
            }
        }

        float yawDelta = Math.abs(data.getLastYawDelta());
        float pitchDelta = Math.abs(data.getLastPitchDelta());
        float yawTolerance = yawDelta > 12.0F ? 1.25F : 0.35F;
        float pitchTolerance = pitchDelta > 4.0F ? 0.75F : 0.25F;
        if (Math.abs(yawDelta - state.lastYawDelta) > yawTolerance
                || Math.abs(pitchDelta - state.lastPitchDelta) > pitchTolerance
                || yawDelta <= 1.5F) {
            state.repeatedPatternCount = 0;
            state.lastYawDelta = yawDelta;
            state.lastPitchDelta = pitchDelta;
            return false;
        }

        state.repeatedPatternCount++;
        state.lastYawDelta = yawDelta;
        state.lastPitchDelta = pitchDelta;
        if (state.repeatedPatternCount < 3) {
            return false;
        }

        double add = 0.45D;
        double buffer = slideAndAddScore(data, add, 1.0D);
        String detail = "REPEATED_ROT yaw=" + fmt(yawDelta) + " pitch=" + fmt(pitchDelta);
        recordEvidence(data, add, "KILLAURA_REPEAT_ROT");
        recordKillAuraCombatEvidence(attacker, data, add, detail);
        if (buffer > plugin.getConfig().getDouble("checks.KillAura.repeated-rotation-buffer", 2.25D)) {
            flag(attacker, data, add, detail);
        }
        return true;
    }

    private boolean checkMultiTarget(Player attacker, PlayerData data, Player target) {
        long now = System.currentTimeMillis();
        long lastAttack = data.getLastAttackAt();
        long timeSince = now - lastAttack;

        boolean suspicious = false;
        if (timeSince > 0 && timeSince < 100 && data.getLastAttackTargetId() != 0
                && data.getLastAttackTargetId() != target.getEntityId()) {
            double add = 0.8D;
            double buffer = slideAndAddScore(data, add, 1.0D);
            String detail = "MULTI switch=" + timeSince + "ms target=" + target.getName();
            recordEvidence(data, add, "KILLAURA_MULTI");
            recordKillAuraCombatEvidence(attacker, data, add, detail);

            if (buffer > plugin.getConfig().getDouble("checks.KillAura.multi-target-buffer", 3.0D)) {
                flag(attacker, data, add, detail);
            }
            suspicious = true;
        }

        data.setLastAttackTargetId(target.getEntityId());
        return suspicious;
    }

    private boolean checkLineOfSight(Player attacker, Player target, PlayerData data,
            ReachCheck.AttackEvaluation reachEval) {
        if (target == null || attacker.hasLineOfSight(target)) {
            return false;
        }
        if (reachEval != null && (!reachEval.isEnforceableWindow() || reachEval.isTeleportMarkerHit())) {
            return false;
        }

        double directDistance = reachEval != null
                ? reachEval.getDirectDistance()
                : attacker.getEyeLocation().distance(target.getEyeLocation());
        if (directDistance < 2.05D) {
            return false;
        }

        double add = reachEval != null && !reachEval.isLegal() ? 0.75D : 0.35D;
        add += Math.min(0.25D, Math.max(0.0D, directDistance - 2.0D) * 0.15D);
        double buffer = slideAndAddScore(data, add, 1.0D);
        String detail = "NO_LOS dist=" + String.format(Locale.ROOT, "%.2f", directDistance);
        recordEvidence(data, add, "KILLAURA_NO_LOS");
        recordKillAuraCombatEvidence(attacker, data, add, detail);
        if (buffer > plugin.getConfig().getDouble("checks.KillAura.wall-buffer", 2.3D)) {
            flag(attacker, data, add, detail);
        }
        return true;
    }

    private boolean checkReachContext(Player attacker, PlayerData data, ReachCheck.AttackEvaluation reachEval) {
        if (reachEval == null || reachEval.isLegal()) {
            return false;
        }

        double add = reachEval.getEvidenceType() == ReachCheck.ReachEvidenceType.HITBOX_MISS ? 0.70D : 0.80D;
        double buffer = slideAndAddScore(data, add, 1.0D);
        String detail = reachEval.getEvidenceType() == ReachCheck.ReachEvidenceType.HITBOX_MISS
                ? "PACKET_HITBOX dist=" + String.format(Locale.ROOT, "%.2f", reachEval.getDirectDistance())
                : "PACKET_REACH dist=" + String.format(Locale.ROOT, "%.2f", reachEval.getDirectDistance());
        recordEvidence(data, add, "KILLAURA_PACKET_CONTEXT");
        recordKillAuraCombatEvidence(attacker, data, add, detail);
        if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
            flag(attacker, data, add, detail);
        }
        return true;
    }

    private void recordKillAuraCombatEvidence(Player attacker, PlayerData data, double score, String detail) {
        Location eye = attacker.getEyeLocation();
        FrameContextSnapshot frameContext = data.getCurrentFrameContext();
        CombatEvidence evidence = CombatEvidence.builder(
                CombatEvidence.CombatCheckType.KILLAURA, attacker.getName(), "")
                .actorPos(eye.getX(), eye.getY(), eye.getZ())
                .actorLook(eye.getYaw(), eye.getPitch())
                .localAttackTime(System.currentTimeMillis())
                .rotation(data.getLastYawDelta(), data.getLastPitchDelta())
                .horizontalDelta(data.getLastDeltaXZ())
                .scoring(score, plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D), score > 0.0D)
                .detail(detail)
                .frameLink(frameContext == null ? -1L : frameContext.getFrameId(),
                        frameContext == null ? -1 : frameContext.getTxWindowId())
                .build();
        data.recordCombatEvidence(evidence);
    }

    private static final class RotationPatternState {
        private float lastYawDelta;
        private float lastPitchDelta;
        private int repeatedPatternCount;
    }

    private String fmt(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
