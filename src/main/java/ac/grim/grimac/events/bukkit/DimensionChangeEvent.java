package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DimensionChangeEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        // How can getTo be null?
        if (event.getTo() != null && event.getFrom().getWorld() != event.getTo().getWorld()) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player != null) {
                player.sendTransaction();
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.isPacketSneaking = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.playerWorld = event.getTo().getWorld());
                player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.playerWorld = event.getTo().getWorld());

                // Force the player to accept a teleport before respawning
                player.getSetbackTeleportUtil().acceptedTeleports = 0;

                if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) && event.getTo().getWorld() != null) {
                    player.compensatedWorld.setMinHeight(event.getTo().getWorld().getMinHeight());
                    player.compensatedWorld.setMaxWorldHeight(event.getTo().getWorld().getMaxHeight());
                }
            }
        }
    }
}
