package ac.grim.legacyac.network.frame;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MovementFrameDispatcher {
    private final LegacyAntiCheatPlugin plugin;

    public MovementFrameDispatcher(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void dispatch(final Player player, final MovementFrame frame) {
        if (!plugin.getConfig().getBoolean("pipeline.packet-first", true)) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            plugin.checks().onMovementFrame(player, frame);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                plugin.checks().onMovementFrame(player, frame);
            }
        });
    }
}
