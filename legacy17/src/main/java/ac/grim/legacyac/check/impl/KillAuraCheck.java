package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Locale;

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

        if (!reachLegal) {
            double buffer = increaseBuffer(data, 0.8D);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, 0.8D, "packet attack not intersecting backtrack hitbox");
            }
        }
    }

    private void checkAngles(Player attacker, PlayerData data, boolean severe) {
        float yawDelta = data.getLastYawDelta();
        float pitchDelta = data.getLastPitchDelta();
        double horizontal = data.getLastDeltaXZ();
        float snapThreshold = (float) plugin.getConfig().getDouble("checks.KillAura.snap-threshold", 75.0D);

        if (yawDelta > snapThreshold && pitchDelta < 2.0F && horizontal < 0.08D) {
            double add = severe ? 1.4D : 1.0D;
            double buffer = increaseBuffer(data, add);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, add, "yawDelta=" + String.format(Locale.ROOT, "%.1f", yawDelta) + " pitchDelta=" + String.format(Locale.ROOT, "%.1f", pitchDelta));
            }
        }
    }
}
