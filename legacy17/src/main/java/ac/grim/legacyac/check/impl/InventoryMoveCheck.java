package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class InventoryMoveCheck extends Check {
    public InventoryMoveCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "InventoryMove");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data) || !data.isInventoryOpen()) {
            return;
        }

        double maxMove = plugin.getConfig().getDouble("checks.InventoryMove.max-move", 0.12D);
        if (data.getLastDeltaXZ() > maxMove) {
            double buffer = increaseBuffer(data, data.getLastDeltaXZ() - maxMove);
            if (buffer > plugin.getConfig().getDouble("checks.InventoryMove.buffer", 0.2D)) {
                flag(player, data, 0.8D, "xz=" + String.format("%.3f", data.getLastDeltaXZ()));
            }
        }
    }
}
