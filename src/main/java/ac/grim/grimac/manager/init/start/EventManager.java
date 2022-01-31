package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.events.bukkit.BedEvent;
import ac.grim.grimac.events.bukkit.PistonEvent;
import ac.grim.grimac.events.bukkit.PlayerJoinQuitListener;
import ac.grim.grimac.events.bukkit.TeleportEvent;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class EventManager implements Initable {
    public void start() {
        LogUtil.info("Registering events...");

        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(), GrimAPI.INSTANCE.getPlugin());

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new BedEvent(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new TeleportEvent(), GrimAPI.INSTANCE.getPlugin());
    }
}
