package org.abyssmc.reaperac;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.abyssmc.reaperac.events.anticheat.GenericMovementCheck;
import org.abyssmc.reaperac.events.bukkit.PlayerJoinLeaveListener;
import org.abyssmc.reaperac.checks.packet.Timer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class ReaperAC extends JavaPlugin {
    public static HashMap<Player, GrimPlayer> playerGrimHashMap = new HashMap<>();
    public static Plugin plugin;
    ProtocolManager manager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        manager = ProtocolLibrary.getProtocolManager();

        registerPackets();

        //PlayerAbilitiesPacket.createListener(this, manager);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            GrimPlayer grimPlayer = new GrimPlayer(player);
            playerGrimHashMap.put(player, new GrimPlayer(player));
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // My hope is to have everything async by using packets!
    public void registerPackets() {
        new Timer(this, manager);
        new GenericMovementCheck(this, manager);
    }
}
