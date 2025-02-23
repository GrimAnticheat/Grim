package ac.grim.bukkit;

import ac.grim.bukkit.command.parsers.BukkitParserDescriptorFactory;
import ac.grim.bukkit.player.BukkitPlatformPlayerFactory;
import ac.grim.bukkit.sender.BukkitSenderFactory;
import ac.grim.bukkit.utils.nms.BukkitNMS;
import ac.grim.bukkit.utils.scheduler.bukkit.BukkitPlatformScheduler;
import ac.grim.bukkit.utils.scheduler.folia.FoliaPlatformScheduler;
import ac.grim.grimac.BasicGrimPlugin;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class GrimACBukkitLoaderPlugin extends JavaPlugin {
    public static GrimACBukkitLoaderPlugin PLUGIN;
    public static BukkitSenderFactory bukkitSenderFactory = null; // todo make final

    @Override
    public void onLoad() {
        GrimAPI.INSTANCE.load(
                new BasicGrimPlugin(this.getLogger(), this.getDataFolder(), this.getDescription().getVersion(),
                        this.getDescription().getDescription(), this.getDescription().getAuthors()),
                GrimAPI.PLATFORM == GrimAPI.Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler(),
                new BukkitPlatformPlayerFactory(),
                SpigotPacketEventsBuilder.build(this),
                new BukkitParserDescriptorFactory(),
                () -> {
                    BukkitSenderFactory bukkitSenderFactoryTemp = new BukkitSenderFactory();
                    LegacyPaperCommandManager<Sender> legacyPaperCommandManager = new LegacyPaperCommandManager<>(
                            this,
                            ExecutionCoordinator.simpleCoordinator(),
                            bukkitSenderFactoryTemp
                    );

                    if (legacyPaperCommandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                        legacyPaperCommandManager.registerBrigadier();
                    } else if (legacyPaperCommandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                        legacyPaperCommandManager.registerAsynchronousCompletions();
                    }
                    bukkitSenderFactory = bukkitSenderFactoryTemp;

                    return legacyPaperCommandManager;
                },
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
