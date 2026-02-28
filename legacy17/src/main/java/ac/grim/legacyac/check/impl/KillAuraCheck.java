package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class KillAuraCheck extends Check {
    public KillAuraCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "KillAura");
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(attacker, data)) {
            return;
        }

        float yawDelta = data.getLastYawDelta();
        float pitchDelta = data.getLastPitchDelta();
        double horizontal = data.getLastDeltaXZ();
        float snapThreshold = (float) plugin.getConfig().getDouble("checks.KillAura.snap-threshold", 75.0D);

        if (yawDelta > snapThreshold && pitchDelta < 2.0F && horizontal < 0.08D) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.KillAura.buffer", 2.0D)) {
                flag(attacker, data, 1.0D, "yawDelta=" + String.format("%.1f", yawDelta) + " pitchDelta=" + String.format("%.1f", pitchDelta));
                event.setCancelled(true);
            }
        }
    }
}
