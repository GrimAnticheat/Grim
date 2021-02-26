package org.abyssmc.reaperac;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.abyssmc.reaperac.bukkitevents.PlayerJoinLeaveListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class ReaperAC extends JavaPlugin {
    public static HashMap<Player, GrimPlayer> playerGrimHashMap = new HashMap<>();

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
            playerGrimHashMap.put(player, new GrimPlayer(player));
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
