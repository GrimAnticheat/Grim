package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class NetworkTapManager implements Listener {
    private final LegacyAntiCheatPlugin plugin;
    private final PacketPipelineInjector injector;

    public NetworkTapManager(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.injector = new PacketPipelineInjector(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("netty.inject-on-join", true)) {
            return;
        }

        final Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                boolean ok = injector.inject(player);
                if (!ok) {
                    plugin.getLogger().warning("[GLAC] Netty inject failed for " + player.getName());
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        injector.uninject(event.getPlayer());
    }

    public void shutdown() {
        injector.uninjectAll();
    }
}
