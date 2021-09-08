package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

public class BedEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedEnterEvent(PlayerBedEnterEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player != null && !event.isCancelled()) {
            player.sendAndFlushTransactionOrPingPong();
            player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.isInBed = true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedExitEvent(PlayerBedLeaveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player != null) {
            player.sendAndFlushTransactionOrPingPong();
            player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.isInBed = false);
        }
    }
}