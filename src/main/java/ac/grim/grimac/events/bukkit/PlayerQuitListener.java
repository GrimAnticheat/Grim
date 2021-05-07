package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        GrimAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
