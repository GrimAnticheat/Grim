package org.abyssmc.reaperac.events.bukkit;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        // TODO: Make this a create player data class (To handle reloads)
        Player player = event.getPlayer();
        GrimPlayer grimPlayer = new GrimPlayer(player);
        grimPlayer.lastX = player.getLocation().getX();
        grimPlayer.lastY = player.getLocation().getY();
        grimPlayer.lastZ = player.getLocation().getZ();
        grimPlayer.lastXRot = player.getLocation().getYaw();
        grimPlayer.lastYRot = player.getLocation().getPitch();

        ReaperAC.playerGrimHashMap.put(event.getPlayer(), new GrimPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        ReaperAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
