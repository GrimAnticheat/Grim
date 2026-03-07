package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.evidence.CombatEvidence;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KillAura detection.
 *
 * Detection methods:
 * 1. Snap detection  large yaw change with minimal pitch change and minimal
 * movement (blatant aura)
 * 2. Packet hitbox miss  USE_ENTITY attack doesn't intersect the backtrack
 * hitbox
 * AND the center distance is beyond reach
 * 3. Multi-hit timing  attacks on multiple targets in rapid succession
 */
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

    public void onUseEntityAttack(Player attacker, Player target, PlayerData data, boolean reachLegal) {
        if (!isEnabled() || isExempt(attacker, data)) {
            return;
        }

        boolean severe = !reachLegal;
        checkAngles(attacker, data, severe);
        checkRepeatedRotationPattern(attacker, data);
        checkMultiTarget(attacker, data, target);

        // Only flag for hitbox miss if reach also says the distance was too far
        // During jumping/fast movement, ray can miss hitbox frames even for legit
        // players
        if (!reachLegal) {
            double buffer = slideAndAddScore(data, 0.8D, 1.0D);
            recordEvidence(data, 0.8D, "KILLAURA_HITBOX");
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, 0.8D, "packet attack not intersecting backtrack hitbox");
            }
        } else {
            // Decay when reach says the attack was legal
            coolDownScore(data);
        }
    }

    /**
     * Detection 1: Snap  large yaw delta with almost no pitch change while barely
     * moving.
     * Catches blatant killaura that snaps to targets.
     */
    private void checkAngles(Player attacker, PlayerData data, boolean severe) {
        float yawDelta = data.getLastYawDelta();
        float pitchDelta = data.getLastPitchDelta();
        double horizontal = data.getLastDeltaXZ();
        float snapThreshold = (float) plugin.getConfig().getDouble("checks.KillAura.snap-threshold", 75.0D);

        // FR-3: Tighten or relax snap threshold based on budget
        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        if (budget != null && budget.getCombatReachMargin() > 0.1D) {
            // Under high lag, widen the snap threshold slightly to avoid false positives
            snapThreshold += 5.0F;
        }

        if (yawDelta > snapThreshold && pitchDelta < 2.0F && horizontal < 0.08D) {
            double add = severe ? 1.4D : 1.0D;
            double buffer = slideAndAddScore(data, add, 1.0D);
            recordEvidence(data, add, "KILLAURA_SNAP");

            // FR-4: Record CombatEvidence
            recordKillAuraCombatEvidence(attacker, data, add,
                    "SNAP yaw=" + fmt(yawDelta) + " pitch=" + fmt(pitchDelta));

            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, add, "SNAP yaw=" + fmt(yawDelta)
                        + " pitch=" + fmt(pitchDelta));
            }
        }
    }

    /**
     * Detection 2: Multi-target switch  attacking multiple different targets in
     * quick succession.
     */
    private void checkRepeatedRotationPattern(Player attacker, PlayerData data) {
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
        if (Math.abs(yawDelta - state.lastYawDelta) < 1.0E-4F && Math.abs(pitchDelta - state.lastPitchDelta) < 1.0E-4F && yawDelta > 2.0F) {
            state.repeatedPatternCount++;
            if (state.repeatedPatternCount >= 3) {
                double buffer = slideAndAddScore(data, 0.45D, 1.0D);
                if (buffer > plugin.getConfig().getDouble("checks.KillAura.repeated-rotation-buffer", 2.25D)) {
                    flag(attacker, data, 0.45D, "REPEATED_ROT yaw=" + fmt(yawDelta) + " pitch=" + fmt(pitchDelta));
                }
            }
        } else {
            state.repeatedPatternCount = 0;
        }
        state.lastYawDelta = yawDelta;
        state.lastPitchDelta = pitchDelta;
    }

    private void checkMultiTarget(Player attacker, PlayerData data, Player target) {
        long now = System.currentTimeMillis();
        long lastAttack = data.getLastAttackAt();
        long timeSince = now - lastAttack;

        if (timeSince > 0 && timeSince < 100 && data.getLastAttackTargetId() != 0
                && data.getLastAttackTargetId() != target.getEntityId()) {
            double deviation = 0.8D;
            double buffer = slideAndAddScore(data, deviation, 1.0D);
            recordEvidence(data, deviation, "KILLAURA_MULTI");

            // FR-4: Record CombatEvidence for multi-target
            recordKillAuraCombatEvidence(attacker, data, deviation,
                    "MULTI switch=" + timeSince + "ms target=" + target.getName());

            if (buffer > plugin.getConfig().getDouble("checks.KillAura.multi-target-buffer", 3.0D)) {
                flag(attacker, data, deviation, "MULTI-TARGET switch=" + timeSince + "ms");
            }
        }

        data.setLastAttackTargetId(target.getEntityId());
    }

    /**
     * FR-4: Build and record a CombatEvidence for KillAura detections.
     */
    private void recordKillAuraCombatEvidence(Player attacker, PlayerData data, double score, String detail) {
        Location eye = attacker.getEyeLocation();
        CombatEvidence evidence = CombatEvidence.builder(
                CombatEvidence.CombatCheckType.KILLAURA, attacker.getName(), "")
                .actorPos(eye.getX(), eye.getY(), eye.getZ())
                .actorLook(eye.getYaw(), eye.getPitch())
                .localAttackTime(System.currentTimeMillis())
                .rotation(data.getLastYawDelta(), data.getLastPitchDelta())
                .horizontalDelta(data.getLastDeltaXZ())
                .scoring(score, plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D), score > 0.0D)
                .detail(detail)
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

