package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GrimPlayer grimPlayer = new GrimPlayer(player);
        grimPlayer.lastX = player.getLocation().getX();
        grimPlayer.lastY = player.getLocation().getY();
        grimPlayer.lastZ = player.getLocation().getZ();
        grimPlayer.lastXRot = player.getLocation().getYaw();
        grimPlayer.lastYRot = player.getLocation().getPitch();
        grimPlayer.x = player.getLocation().getX();
        grimPlayer.y = player.getLocation().getY();
        grimPlayer.z = player.getLocation().getZ();
        grimPlayer.xRot = player.getLocation().getYaw();
        grimPlayer.yRot = player.getLocation().getPitch();

        GrimAC.playerGrimHashMap.put(event.getPlayer(), new GrimPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        GrimAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
