package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.entity.Player;

public final class TimerCheck extends Check {
    public TimerCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Timer");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data)) {
            return;
        }

        int maxMoves = plugin.getConfig().getInt("checks.Timer.max-moves-per-second", 26);
        if (data.getMoveWindow() > maxMoves) {
            double buffer = increaseBuffer(data, 0.5D);
            if (buffer > plugin.getConfig().getDouble("checks.Timer.buffer", 2.0D)) {
                flag(player, data, 0.5D, "moves=" + data.getMoveWindow());
            }
        }
    }
}
