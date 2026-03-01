package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Locale;

/**
 * KillAura detection.
 *
 * Detection methods:
 * 1. Snap detection — large yaw change with minimal pitch change and minimal movement (blatant aura)
 * 2. Packet hitbox miss — USE_ENTITY attack doesn't intersect the backtrack hitbox
 *    AND the center distance is beyond reach
 * 3. Multi-hit timing — attacks on multiple targets in rapid succession
 */
public final class KillAuraCheck extends Check {
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
        checkMultiTarget(attacker, data, target);

        // Only flag for hitbox miss if reach also says the distance was too far
        // During jumping/fast movement, ray can miss hitbox frames even for legit players
        if (!reachLegal) {
            double buffer = slideAndAddScore(data, 0.8D, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, 0.8D, "packet attack not intersecting backtrack hitbox");
            }
        } else {
            // Decay when reach says the attack was legal
            coolDownScore(data);
        }
    }

    /**
     * Detection 1: Snap — large yaw delta with almost no pitch change while barely moving.
     * Catches blatant killaura that snaps to targets.
     */
    private void checkAngles(Player attacker, PlayerData data, boolean severe) {
        float yawDelta = data.getLastYawDelta();
        float pitchDelta = data.getLastPitchDelta();
        double horizontal = data.getLastDeltaXZ();
        float snapThreshold = (float) plugin.getConfig().getDouble("checks.KillAura.snap-threshold", 75.0D);

        if (yawDelta > snapThreshold && pitchDelta < 2.0F && horizontal < 0.08D) {
            double add = severe ? 1.4D : 1.0D;
            double buffer = slideAndAddScore(data, add, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, add, "SNAP yaw=" + fmt(yawDelta)
                    + " pitch=" + fmt(pitchDelta));
            }
        }
    }

    /**
     * Detection 2: Multi-target switch — attacking multiple different targets in quick succession.
     */
    private void checkMultiTarget(Player attacker, PlayerData data, Player target) {
        long now = System.currentTimeMillis();
        long lastAttack = data.getLastAttackAt();
        long timeSince = now - lastAttack;

        if (timeSince > 0 && timeSince < 100 && data.getLastAttackTargetId() != 0
            && data.getLastAttackTargetId() != target.getEntityId()) {
            double deviation = 0.8D;
            double buffer = slideAndAddScore(data, deviation, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.multi-target-buffer", 3.0D)) {
                flag(attacker, data, deviation, "MULTI-TARGET switch=" + timeSince + "ms");
            }
        }

        data.setLastAttackTargetId(target.getEntityId());
    }

    private String fmt(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
