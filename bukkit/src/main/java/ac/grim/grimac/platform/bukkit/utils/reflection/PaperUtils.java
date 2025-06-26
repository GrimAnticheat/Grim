package ac.grim.grimac.platform.bukkit.utils.reflection;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.concurrent.CompletableFuture;

public class PaperUtils {
    public static final boolean PAPER = ReflectionUtils.hasClass("com.destroystokyo.paper.PaperConfig")
            || ReflectionUtils.hasClass("io.papermc.paper.configuration.Configuration");

    public static CompletableFuture<Boolean> teleportAsync(final Entity entity, final Location location) {
        return PAPER ? entity.teleportAsync(location) : CompletableFuture.completedFuture(entity.teleport(location));
    }

    @SuppressWarnings("unchecked")
    public static boolean registerTickEndEvent(Listener listener, Runnable runnable) {
        try {
            Class<?> clazz = ReflectionUtils.getClass("com.destroystokyo.paper.event.server.ServerTickEndEvent");
            if (clazz == null) return false;
            GrimACBukkitLoaderPlugin.LOADER.getServer().getPluginManager().registerEvent(
                    (Class<? extends Event>) clazz,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> runnable.run(),
                    GrimACBukkitLoaderPlugin.LOADER
            );
            return true;
        } catch (Exception e) {
            LogUtil.error("Failed to register tick end event", e);
        }
        return false;
    }
}
