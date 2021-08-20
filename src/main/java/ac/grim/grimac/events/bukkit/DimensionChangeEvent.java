package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DimensionChangeEvent implements Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        // How can getTo be null?
        if (event.getTo() != null && event.getFrom().getWorld() != event.getTo().getWorld()) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player != null) {
                player.sendAndFlushTransactionOrPingPong();
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.isPacketSneaking = false);
                player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.playerWorld = event.getTo().getWorld());
            }
        }
    }
}
