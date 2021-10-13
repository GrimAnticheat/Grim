package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
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

        // Don't let the vanilla anticheat override our teleports
        // Revision 5.
        //
        // We check the log for whether the vanilla anticheat warned that the player moved too quickly
        // If so, we ignore the bukkit events and cancel the first netty packet for a teleport
        //
        // We do this by the following (fuck you md_5 for "fixing" that teleport on join bug and messing up the entire teleports system):
        // 1) If we are lucky enough to get a god-damn teleport event, we are safe and can simply ignore the first bukkit teleport
        // set vanillaAC to false, and continue on.
        // 2) If we don't get a bukkit teleport, we try to handle this by not doing this logic for not UNKNOWN teleports,
        // so that we don't override a plugin teleport.  UNKNOWN teleports are very rare on modern versions with this bugfix
        // (nice bug fix MD_5).  We then wait until the first unknown netty teleport that didn't call this teleport event
        // because of MD_5's glorious bugfix, and then cancel it.  It isn't perfect :( but I think it should
        // work to be MOSTLY synchronous correct.  Vehicle teleports MAY still cause issues if it's a tick within
        // the vanilla anticheat, but I don't think it will lead to any bypasses
        if (to != null) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // This was the vanilla anticheat, teleport the player back!
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN && player.vanillaACTeleports > 0) {
                player.vanillaACTeleports--;
                event.setCancelled(true);
                player.getSetbackTeleportUtil().teleportPlayerToOverrideVanillaAC();
                return;
            }

            player.getSetbackTeleportUtil().setTargetTeleport(to);
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
        player.getSetbackTeleportUtil().setTargetTeleport(loc);

        onWorldChangeEvent(player, event.getRespawnLocation().getWorld());
    }

    private void onWorldChangeEvent(GrimPlayer player, World newWorld) {
        if (player == null) return;

        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            player.packetStateData.isPacketSneaking = false;
        });
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.playerWorld = newWorld);
        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.playerWorld = newWorld);

        // Force the player to accept a teleport before respawning
        player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport = false;

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) && newWorld != null) {
            player.compensatedWorld.setMinHeight(newWorld.getMinHeight());
            player.compensatedWorld.setMaxWorldHeight(newWorld.getMaxHeight());
        }
    }
}
