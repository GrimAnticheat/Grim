package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ReachCheck extends Check {
    public ReachCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Reach");
    }

    public void onAttack(EntityDamageByEntityEvent event, Player attacker, Player victim, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(attacker, data)) {
            return;
        }

        Location eye = attacker.getEyeLocation();
        Location target = victim.getLocation().add(0.0D, 1.0D, 0.0D);
        double distance = eye.distance(target);
        double maxReach = plugin.getConfig().getDouble("checks.Reach.max-distance", 3.35D);

        if (distance > maxReach) {
            double buffer = increaseBuffer(data, distance - maxReach);
            if (buffer > plugin.getConfig().getDouble("checks.Reach.buffer", 0.2D)) {
                flag(attacker, data, distance - maxReach, "dist=" + String.format("%.3f", distance));
                event.setCancelled(true);
            }
        }
        data.setLastAttackAt(System.currentTimeMillis());
    }
}
