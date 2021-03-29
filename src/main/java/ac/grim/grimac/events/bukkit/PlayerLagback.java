package ac.grim.grimac.events.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.UUID;

public class PlayerLagback implements Listener {
    // TODO: Make this a weak reference or otherwise stop memory leaks
    public static HashSet<UUID> playersToLagback = new HashSet<>();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (playersToLagback.remove(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
