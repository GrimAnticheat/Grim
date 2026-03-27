package ac.grim.legacyac.check.impl.breaking;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public final class AirLiquidBreakCheck extends Check {
    public AirLiquidBreakCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AirLiquidBreak");
    }

    public void onBreak(BlockBreakEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        Material type = event.getBlock().getType();
        boolean invalid = type == Material.AIR
                || type == Material.WATER || type == Material.STATIONARY_WATER
                || type == Material.LAVA || type == Material.STATIONARY_LAVA
                || type == Material.WEB;
        if (invalid) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.AirLiquidBreak.buffer", 1.0D)) {
                flag(event.getPlayer(), data, 1.0D, "block=" + type.name());
                event.setCancelled(true);
            }
        }
    }

    public void onPacketBreak(Player player, PlayerData data, PlayerData.QueuedBlockDigSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }
        Material type = data.getCompensatedBlockType(player, snapshot.getX(), snapshot.getY(), snapshot.getZ());
        boolean invalid = type == Material.AIR
                || type == Material.WATER || type == Material.STATIONARY_WATER
                || type == Material.LAVA || type == Material.STATIONARY_LAVA
                || type == Material.WEB;
        if (invalid) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.AirLiquidBreak.buffer", 1.0D)) {
                flag(player, data, 1.0D, "block=" + type.name());
            }
        }
    }
}
