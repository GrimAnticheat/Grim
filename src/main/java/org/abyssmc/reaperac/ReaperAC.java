package org.abyssmc.reaperac;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.abyssmc.reaperac.players.GrimPlayer;
import org.abyssmc.reaperac.players.GrimPlayerManager;
import org.abyssmc.reaperac.players.PlayerJoinLeaveListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReaperAC extends JavaPlugin {
    ProtocolManager manager;
    public static Plugin plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        manager = ProtocolLibrary.getProtocolManager();
        //PlayerAbilitiesPacket.createListener(this, manager);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);

        for (Player player: Bukkit.getOnlinePlayers()) {
            GrimPlayer grimPlayer = new GrimPlayer(player);
            Bukkit.getPluginManager().registerEvents(grimPlayer, ReaperAC.plugin);
            GrimPlayerManager.playerGrimHashMap.put(player, new GrimPlayer(player));
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
