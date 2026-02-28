package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class JesusCheck extends Check {
    public JesusCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Jesus");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        Block feet = player.getLocation().getBlock();
        Block below = player.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        boolean liquidNearby = feet.getType() == Material.WATER || feet.getType() == Material.STATIONARY_WATER
            || below.getType() == Material.WATER || below.getType() == Material.STATIONARY_WATER;

        if (!liquidNearby) {
            return;
        }

        double maxWaterSpeed = plugin.getConfig().getDouble("checks.Jesus.max-water-speed", 0.18D);
        if (data.getLastDeltaXZ() > maxWaterSpeed && Math.abs(data.getLastDeltaY()) < 0.02D) {
            double buffer = increaseBuffer(data, data.getLastDeltaXZ() - maxWaterSpeed);
            if (buffer > plugin.getConfig().getDouble("checks.Jesus.buffer", 0.25D)) {
                flag(player, data, 1.0D, "waterXZ=" + String.format("%.3f", data.getLastDeltaXZ()));
            }
        }
    }
}
