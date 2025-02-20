package ac.grim.bukkit;

import ac.grim.bukkit.player.BukkitPlatformPlayerFactory;
import ac.grim.bukkit.utils.nms.BukkitNMS;
import ac.grim.bukkit.utils.scheduler.bukkit.BukkitPlatformScheduler;
import ac.grim.bukkit.utils.scheduler.folia.FoliaPlatformScheduler;
import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public final class GrimACBukkitLoaderPlugin extends JavaPlugin {
    public static GrimACBukkitLoaderPlugin PLUGIN;

    @Override
    public void onLoad() {
        GrimAPI.INSTANCE.load(
                new BasicGrimPlugin(this.getLogger(), this.getDataFolder(), this.getDescription().getVersion(), this.getDescription().getDescription(), this.getDescription().getAuthors()),
                GrimAPI.PLATFORM == GrimAPI.Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler(),
                new BukkitPlatformPlayerFactory(),
                SpigotPacketEventsBuilder.build(this),
                BukkitNMS::new
        );
    }

    @Override
    public void onDisable() {
        GrimAPI.INSTANCE.stop();
        PLUGIN = null; // Reset on disable for safety
    }

    @Override
    public void onEnable() {
        if (PLUGIN != null) {
            throw new IllegalStateException("GrimAC Bukkit plugin has already been initialized!");
        }
        PLUGIN = this;
        GrimAPI.INSTANCE.start();
    }
}
