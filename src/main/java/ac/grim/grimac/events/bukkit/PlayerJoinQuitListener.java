package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.GrimAlerts;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("grim.alerts")) {
            if (GrimAPI.INSTANCE.getPlugin().getConfig().getBoolean("alerts.enable-on-join")) {
                String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("messages.alerts-enabled", "%prefix% &fAlerts &benabled");
                event.getPlayer().sendMessage(MessageUtil.format(alertString));
            } else {
                GrimAlerts.toggle(event.getPlayer());
            }
        }
    }

    // PacketEvents uses priority HIGHEST
    @EventHandler(priority = EventPriority.HIGH)
    public void playerQuitEvent(PlayerQuitEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) return;
        User user = PacketEvents.getAPI().getPlayerManager().getUser(event.getPlayer());
        GrimAPI.INSTANCE.getPlayerDataManager().remove(user);
        GrimAlerts.handlePlayerQuit(event.getPlayer());
    }
}
