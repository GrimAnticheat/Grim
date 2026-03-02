package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Locale;

public final class InventoryMoveCheck extends Check {
    public InventoryMoveCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "InventoryMove");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data) || !data.isInventoryOpen()) {
            return;
        }

        if (System.currentTimeMillis() - data.getInventoryOpenAt() < 1500L) {
            return;
        }

        double maxMove = plugin.getConfig().getDouble("checks.InventoryMove.max-move", 0.12D);
        if (data.getLastDeltaXZ() > maxMove) {
            double buffer = increaseBuffer(data, data.getLastDeltaXZ() - maxMove);
            if (buffer > plugin.getConfig().getDouble("checks.InventoryMove.buffer", 0.2D)) {
                flag(player, data, 0.8D, "xz=" + String.format(Locale.ROOT, "%.3f", data.getLastDeltaXZ()));
            }
        }
    }
}
