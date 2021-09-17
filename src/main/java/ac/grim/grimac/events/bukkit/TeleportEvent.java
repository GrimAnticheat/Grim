package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();

        // If the teleport is not from vanilla anticheat
        if (to != null && (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ())) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;
            player.getSetbackTeleportUtil().setSetback(new Vector3d(to.getX(), to.getY(), to.getZ()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        Location loc = event.getRespawnLocation();
        player.getSetbackTeleportUtil().setSetback(new Vector3d(loc.getX(), loc.getY(), loc.getZ()));

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.playerWorld = event.getRespawnLocation().getWorld());
        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.playerWorld = event.getRespawnLocation().getWorld());

        // Force the player to accept a teleport before respawning
        player.getSetbackTeleportUtil().acceptedTeleports = 0;

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) && event.getRespawnLocation().getWorld() != null) {
            player.compensatedWorld.setMinHeight(event.getRespawnLocation().getWorld().getMinHeight());
            player.compensatedWorld.setMaxWorldHeight(event.getRespawnLocation().getWorld().getMaxHeight());
        }
    }
}
