package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.AbstractTickEndEvent;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayer;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.lists.HookedListWrapper;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

// Copied from: https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/inject/ServerInjector.java
@SuppressWarnings(value = {"unchecked", "deprecated"})
public class BukkitTickEndEvent extends AbstractTickEndEvent implements Listener {

    private Boolean getLateBindState() {
        Class<?> spigotConfig = ReflectionUtils.getClass("org.spigotmc.SpigotConfig");
        // ReflectionUtils.getField(class, name) handles the loop and setAccessible
        Field field = ReflectionUtils.getField(spigotConfig, "lateBind");

        if (field == null) return null;

        try {
            return (boolean) field.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_11_2) && !Boolean.getBoolean("paper.explicit-flush")) {
            LogUtil.warn("Reach.enable-post-packet=true but paper.explicit-flush=false, add \"-Dpaper.explicit-flush=true\" to your server's startup flags for fully functional extra reach accuracy.");
        }
        // this is necessary for folia
        if (GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            PaperUtils.registerTickEndEvent(this, this::tickAllFoliaPlayers);
            return;
        }
        // if it fails to register Paper event, try to inject via reflection
        if (!PaperUtils.registerTickEndEvent(this, this::tickAllPlayers) && !injectWithReflection()) {
            LogUtil.error("Failed to inject into the end of tick event!");

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_14_4)) {
                Boolean lateBind = getLateBindState();
                if (lateBind == null) {
                    LogUtil.error("Failed to determine the late-bind state. Perhaps you are using a custom server fork? Check the fork configuration for a late-bind option and disable it.");
                } else if (lateBind) {
                    LogUtil.error("Injection failed because the late-bind option is enabled. Disable it in spigot.yml.");
                }
            }
        }
    }

    private void tickAllPlayers() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableGrim) continue;
            super.onEndOfTick(player);
        }
    }

    private void tickAllFoliaPlayers() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableGrim) continue;
            if (player.platformPlayer == null) continue;
            Player p = ((BukkitPlatformPlayer) player.platformPlayer).getNative();
            if (!Bukkit.isOwnedByCurrentRegion(p)) continue;
            super.onEndOfTick(player);
        }
    }

    private boolean injectWithReflection() {
        // Inject so we can add the final transaction pre-flush event
        try {
            Object connection = SpigotReflectionUtil.getMinecraftServerConnectionInstance();
            if (connection == null) return false;

            Field connectionsList = Reflection.getField(connection.getClass(), List.class, 1);
            List<Object> endOfTickObject = (List<Object>) connectionsList.get(connection);

            // Use a list wrapper to check when the size method is called
            // Unsure why synchronized is needed because the object itself gets synchronized
            // but whatever.  At least plugins can't break it, I guess.
            //
            // Pledge injects into another list, so we should be safe injecting into this one
            List<?> wrapper = Collections.synchronizedList(new HookedListWrapper<>(endOfTickObject) {
                @Override
                public void onIterator() {
                    tickAllPlayers();
                }
            });

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObject(connection, unsafe.objectFieldOffset(connectionsList), wrapper);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LogUtil.error("Failed to inject into the end of tick event via reflection", e);
            return false;
        }
        return true;
    }
}
