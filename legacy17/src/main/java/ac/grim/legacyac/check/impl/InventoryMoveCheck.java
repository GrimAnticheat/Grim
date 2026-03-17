package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class InventoryMoveCheck extends Check {
    public InventoryMoveCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "InventoryMove");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(),
                event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(),
                true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long openGraceMs = plugin.getConfig().getLong("checks.InventoryMove.open-grace-ms", 1500L);
        long closeGraceMs = plugin.getConfig().getLong("checks.InventoryMove.close-grace-ms", 250L);
        if (isExempt(player, data) || !data.isInventoryOpen()) {
            resetBuffer(data);
            return;
        }

        if ((now - data.getInventoryOpenAt()) < openGraceMs || (now - data.getInventoryCloseAt()) < closeGraceMs) {
            resetBuffer(data);
            return;
        }

        double maxMove = plugin.getConfig().getDouble("checks.InventoryMove.max-move", 0.12D);
        if (data.getLastDeltaXZ() <= maxMove) {
            coolDownScore(data);
            return;
        }

        double deviation = data.getLastDeltaXZ() - maxMove;
        double weight = plugin.getConfig().getDouble("checks.InventoryMove.window-weight", 1.0D);
        double buffer = slideAndAddScore(data, deviation, weight);
        if (buffer > plugin.getConfig().getDouble("checks.InventoryMove.buffer", 0.2D)) {
            flag(player, data, 0.8D, "xz=" + String.format(Locale.ROOT, "%.3f", data.getLastDeltaXZ()));
        }
    }

    private void resetBuffer(PlayerData data) {
        data.setBuffer(getName(), 0.0D);
    }
}
