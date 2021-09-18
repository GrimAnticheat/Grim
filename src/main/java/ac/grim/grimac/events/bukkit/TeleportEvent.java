package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Location;
import org.bukkit.World;
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
        // (Vanilla anticheat has a teleport cause of UNKNOWN)
        if (to != null  && event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;
            player.getSetbackTeleportUtil().setSetback(new Vector3d(to.getX(), to.getY(), to.getZ()));
        }

        // How can getTo be null?
        if (event.getTo() != null && event.getFrom().getWorld() != event.getTo().getWorld()) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            onWorldChangeEvent(player, event.getTo().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        Location loc = event.getRespawnLocation();
        player.getSetbackTeleportUtil().setSetback(new Vector3d(loc.getX(), loc.getY(), loc.getZ()));

        onWorldChangeEvent(player, event.getRespawnLocation().getWorld());
    }

    private void onWorldChangeEvent(GrimPlayer player, World newWorld) {
        if (player == null) return;

        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            player.packetStateData.isPacketSneaking = false;
            player.packetStateData.isPacketSprinting = false;
        });
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.playerWorld = newWorld);
        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.playerWorld = newWorld);

        // Force the player to accept a teleport before respawning
        player.getSetbackTeleportUtil().acceptedTeleports = 0;

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) && newWorld != null) {
            player.compensatedWorld.setMinHeight(newWorld.getMinHeight());
            player.compensatedWorld.setMaxWorldHeight(newWorld.getMaxHeight());
        }
    }
}
