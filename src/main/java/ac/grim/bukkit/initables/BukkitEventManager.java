package ac.grim.bukkit.initables;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.bukkit.events.PistonEvent;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class BukkitEventManager implements Initable {
    public void start() {
        LogUtil.info("Registering singular bukkit event... (PistonEvent)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), GrimACBukkitLoaderPlugin.PLUGIN);
    }
}
