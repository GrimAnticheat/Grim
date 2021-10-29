package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Location;
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
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                Location bedPos = event.getBed().getLocation();
                player.bedPosition = new Vector3d(bedPos.getBlockX() + 0.5, bedPos.getBlockY() + 0.5, bedPos.getBlockZ() + 0.5);
                player.isInBed = true;
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedExitEvent(PlayerBedLeaveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player != null) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.isInBed = false);
        }
    }
}