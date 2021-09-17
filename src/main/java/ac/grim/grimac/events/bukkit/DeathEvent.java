package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getEntity());
        if (player != null) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.isDead = true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player != null) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.isDead = false);
        }
    }
}
