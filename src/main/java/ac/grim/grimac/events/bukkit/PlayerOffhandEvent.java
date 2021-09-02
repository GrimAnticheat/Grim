package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerOffhandEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onOffhandEvent(PlayerSwapHandItemsEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.tickSinceLastOffhand = 0);
    }
}
