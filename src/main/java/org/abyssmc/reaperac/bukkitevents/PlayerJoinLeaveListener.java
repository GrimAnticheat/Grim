package org.abyssmc.reaperac.bukkitevents;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        GrimPlayer grimPlayer = new GrimPlayer(event.getPlayer());
        Bukkit.getPluginManager().registerEvents(grimPlayer, ReaperAC.plugin);
        ReaperAC.playerGrimHashMap.put(event.getPlayer(), new GrimPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        GrimPlayer grimPlayer = ReaperAC.playerGrimHashMap.get(event.getPlayer());
        HandlerList.unregisterAll(grimPlayer);
        ReaperAC.playerGrimHashMap.remove(event.getPlayer());
    }
}
