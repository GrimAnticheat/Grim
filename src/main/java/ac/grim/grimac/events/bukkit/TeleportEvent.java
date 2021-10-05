package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
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
        // Revision 3.
        //
        // This works because through 1.7-1.17, the packet that the player can send to trigger the vanilla ac
        // is quite obviously, the position packet.
        //
        // This doesn't break vanilla commands as those are done with the TPCommand etc.
        // This doesn't break vehicles as those are done with use entity packet
        //
        // A plugin can technically call this event with the unknown cause
        // on the player move event and, it would falsely trigger this protection
        // (never seen this, it would have to be explicit, and plugins by default use and should use PLUGIN cause)
        //
        boolean wasVanillaAntiCheat = false;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            StackTraceElement[] elements = new Exception().getStackTrace();
            for (StackTraceElement element : elements) {
                if (element.getClassName().substring(element.getClassName().lastIndexOf(".") + 1).startsWith("PacketPlayInFlying")) {
                    wasVanillaAntiCheat = true;
                    break;
                }
            }
        }

        if (wasVanillaAntiCheat) {
            LogUtil.info(event.getPlayer().getName() + " triggered vanilla anticheat, overriding to stop abuse!");
        }

        // If the teleport is not from vanilla anticheat
        // (Vanilla anticheat has a teleport cause of UNKNOWN)
        if (to != null && !wasVanillaAntiCheat) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;
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
        player.getSetbackTeleportUtil().acceptedTeleports = 0;

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) && newWorld != null) {
            player.compensatedWorld.setMinHeight(newWorld.getMinHeight());
            player.compensatedWorld.setMaxWorldHeight(newWorld.getMaxHeight());
        }
    }
}
