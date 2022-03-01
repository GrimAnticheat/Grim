package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {
    // PacketEvents uses priority HIGHEST
    @EventHandler(priority = EventPriority.HIGH)
    public void playerQuitEvent(PlayerQuitEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) return;
        User user = PacketEvents.getAPI().getPlayerManager().getUser(event.getPlayer());
        GrimAPI.INSTANCE.getPlayerDataManager().remove(user);
    }
}
