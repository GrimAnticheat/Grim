package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerJoinEvent(PlayerJoinEvent event) {
        // Only add the player if they weren't added by the teleport handler yet
        if (GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer()) == null) {
            new GrimPlayer(event.getPlayer());
        }

        // Force the player to resync their sprinting status
        // Fixes false after transferring from a proxy, as both bungee and velocity don't handle
        // the sprinting state correctly and inform us.
        // (This will hardcrash the server if we do it in the grimplayer object and the teleport handler)
        event.getPlayer().setSprinting(true);
        event.getPlayer().setSprinting(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerQuitEvent(PlayerQuitEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getPlayer());
    }
}
