package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.checks.predictionengine.MovementCheckRunner;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerJoinQuitListener implements Listener {

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        Player bukkitPlayer = event.getPlayer();
        GrimPlayer player = new GrimPlayer(bukkitPlayer);
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

        GrimAC.playerGrimHashMap.put(event.getPlayer(), player);

        MovementCheckRunner.queuedPredictions.put(event.getPlayer().getUniqueId(), new ConcurrentLinkedQueue<>());
    }

    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent event) {
        MovementCheckRunner.queuedPredictions.remove(event.getPlayer().getUniqueId());
    }
}
