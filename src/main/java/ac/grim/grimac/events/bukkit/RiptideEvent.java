package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

public class RiptideEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRiptideEvent(PlayerRiptideEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

        if (player == null) return;

        player.compensatedRiptide.addRiptide();
    }
}