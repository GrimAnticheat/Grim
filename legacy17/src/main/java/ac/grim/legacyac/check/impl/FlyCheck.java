package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Locale;

public final class FlyCheck extends Check {
    public FlyCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Fly");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }
        if (player.isFlying() || player.getAllowFlight() || player.getVehicle() != null) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }

        if (player.isOnGround() || event.getTo().getBlock().getType() == Material.WATER || event.getTo().getBlock().getType() == Material.LAVA) {
            return;
        }

        boolean suspiciousHover = data.getAirTicks() > plugin.getConfig().getInt("checks.Fly.air-ticks-threshold", 10)
            && Math.abs(data.getLastDeltaY()) < plugin.getConfig().getDouble("checks.Fly.max-dy", 0.02D);
        boolean parabolaBroken = data.isParabolaAnomalous(
            plugin.getConfig().getDouble("checks.Fly.parabola-error-threshold", 0.03D),
            plugin.getConfig().getInt("checks.Fly.parabola-min-samples", 6)
        ) && data.getShadowDeviation() > plugin.getConfig().getDouble("checks.Fly.shadow-min-deviation", 0.12D);

        if (suspiciousHover || parabolaBroken) {
            double deviation = suspiciousHover ? 1.0D : 0.8D;
            double buffer = slideAndAddScore(data, deviation, plugin.getConfig().getDouble("checks.Fly.window-weight", 1.0D));
            if (buffer > plugin.getConfig().getDouble("checks.Fly.buffer", 2.0D)) {
                flag(player, data, deviation, "airTicks=" + data.getAirTicks() + " dy=" + String.format(Locale.ROOT, "%.4f", data.getLastDeltaY()));
            }
        } else {
            coolDownScore(data);
        }
    }
}
