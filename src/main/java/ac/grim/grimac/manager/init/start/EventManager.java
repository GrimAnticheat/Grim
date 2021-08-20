package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.events.bukkit.*;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Bukkit;

public class EventManager implements Initable {
    public void start() {
        LogUtil.info("Registering events...");

        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(), GrimAPI.INSTANCE.getPlugin());

        if (XMaterial.isNewVersion()) {
            Bukkit.getPluginManager().registerEvents(new FlatPlayerBlockBreakPlace(), GrimAPI.INSTANCE.getPlugin());
        } else {
            Bukkit.getPluginManager().registerEvents(new MagicPlayerBlockBreakPlace(), GrimAPI.INSTANCE.getPlugin());
        }

        if (XMaterial.supports(9)) {
            Bukkit.getPluginManager().registerEvents(new PlayerToggleElytra(), GrimAPI.INSTANCE.getPlugin());
        }

        if (XMaterial.supports(13)) {
            Bukkit.getPluginManager().registerEvents(new RiptideEvent(), GrimAPI.INSTANCE.getPlugin());
        }

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new PlayerConsumeItem(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new DimensionChangeEvent(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new GamemodeChangeEvent(), GrimAPI.INSTANCE.getPlugin());
        Bukkit.getPluginManager().registerEvents(new BedEvent(), GrimAPI.INSTANCE.getPlugin());
    }
}
