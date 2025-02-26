package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements Initable {
    @Override
    public void start() {
        int pluginId = 12820; // <-- Replace with the id of your plugin!
        try {
            new Metrics(GrimACBukkitLoaderPlugin.PLUGIN, pluginId);
        } catch (Exception ignored) {
        }
    }
}
