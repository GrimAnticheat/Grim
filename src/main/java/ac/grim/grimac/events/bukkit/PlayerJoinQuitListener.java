package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerJoinQuitListener implements Listener {

    public static boolean isViaLegacyUpdated = true;

    // Allow other plugins to modify login location or flight status
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerJoinEvent(PlayerJoinEvent event) {
        Player bukkitPlayer = event.getPlayer();

        if (PacketEvents.get().getPlayerUtils().isGeyserPlayer(bukkitPlayer)) return;

        GrimPlayer player = new GrimPlayer(bukkitPlayer);

        // We can't send transaction packets to this player, disable the anticheat for them
        if (!isViaLegacyUpdated && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_16_4)) {
            GrimAC.staticGetLogger().warning(ChatColor.RED + "Please update ViaBackwards to 4.0.2 or newer");
            GrimAC.staticGetLogger().warning(ChatColor.RED + "An important packet is broken for 1.16 and below clients on this ViaBackwards version");
            GrimAC.staticGetLogger().warning(ChatColor.RED + "Disabling all checks for 1.16 and below players as otherwise they WILL be falsely banned");
            GrimAC.staticGetLogger().warning(ChatColor.RED + "Supported version: " + ChatColor.WHITE + "https://github.com/ViaVersion/ViaBackwards/actions/runs/1039987269");
            return;
        }

        player.lastX = bukkitPlayer.getLocation().getX();
        player.lastY = bukkitPlayer.getLocation().getY();
        player.lastZ = bukkitPlayer.getLocation().getZ();
        player.lastXRot = bukkitPlayer.getLocation().getYaw();
        player.lastYRot = bukkitPlayer.getLocation().getPitch();
        player.x = bukkitPlayer.getLocation().getX();
        player.y = bukkitPlayer.getLocation().getY();
        player.z = bukkitPlayer.getLocation().getZ();
        player.xRot = bukkitPlayer.getLocation().getYaw();
        player.yRot = bukkitPlayer.getLocation().getPitch();

        player.packetStateData.packetPlayerX = bukkitPlayer.getLocation().getX();
        player.packetStateData.packetPlayerY = bukkitPlayer.getLocation().getY();
        player.packetStateData.packetPlayerZ = bukkitPlayer.getLocation().getZ();
        player.packetStateData.packetPlayerXRot = bukkitPlayer.getLocation().getYaw();
        player.packetStateData.packetPlayerYRot = bukkitPlayer.getLocation().getPitch();

        player.uncertaintyHandler.pistonPushing.add(0d);
        player.uncertaintyHandler.collidingEntities.add(0);
        player.uncertaintyHandler.tempElytraFlightHack.add(false);
        player.uncertaintyHandler.stuckMultiplierZeroPointZeroThree.add(false);

        GrimAC.playerGrimHashMap.put(event.getPlayer(), player);

        MovementCheckRunner.queuedPredictions.put(event.getPlayer().getUniqueId(), new ConcurrentLinkedQueue<>());
    }

    // Better compatibility with other plugins that use our API
    @EventHandler(priority = EventPriority.HIGH)
    public void playerQuitEvent(PlayerQuitEvent event) {
        MovementCheckRunner.queuedPredictions.remove(event.getPlayer().getUniqueId());
        GrimAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
