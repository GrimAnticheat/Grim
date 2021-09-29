package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.lists.HookedListWrapper;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Copied from: https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/inject/ServerInjector.java
@SuppressWarnings(value = {"unchecked", "deprecated"})
public class TickEndEvent implements Initable {
    static Class<?> tickEnd = null;

    static {
        try {
            if (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_8_8)) {
                tickEnd = NMSUtils.getNMSClass("IUpdatePlayerListBox");
            } else if (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_13_2)) {
                tickEnd = NMSUtils.getNMSClass("ITickable");
            } else {
                tickEnd = Runnable.class;
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    boolean hasTicked = true;

    private static void tickRelMove() { // Don't send packets on the main thread.
        CompletableFuture.runAsync(() -> {
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                player.checkManager.getReach().onEndOfTickEvent();
            }
        }, MovementCheckRunner.executor);
    }

    @Override
    public void start() {
        Field endOfTickList = Reflection.getField(NMSUtils.minecraftServerClass, List.class, 0);
        Object server = NMSUtils.getMinecraftServerInstance(Bukkit.getServer());

        try {
            List<Object> endOfTickObject = (List<Object>) endOfTickList.get(server);

            // Use a list wrapper to check when the size method is called
            HookedListWrapper<?> wrapper = new HookedListWrapper<Object>(endOfTickObject) {
                @Override
                public void onSize() {
                    hasTicked = true;
                    tickRelMove();
                }
            };

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObject(server, unsafe.objectFieldOffset(endOfTickList), wrapper);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // This should NEVER happen!  But there are two scenarios where it could:
        // 1) Some stupid jar messed up our reflection
        // 2) Some stupid jar doesn't tick the list at the end for "optimization"
        // 3) Some stupid jar removed the list at the end because it wasn't needed
        //
        // Otherwise, this is just redundancy.  If the end of tick event isn't firing, this will
        // at the beginning of the next tick so relative moves are still sent.
        Bukkit.getScheduler().runTaskTimer(GrimAPI.INSTANCE.getPlugin(), () -> {
            if (!hasTicked) {
                LogUtil.warn("End of tick hook did not fire... please make a ticket about this. Recovering!");
                tickRelMove();
            }

            hasTicked = false;
        }, 2, 1); // give the server a chance to tick, delay by 2 ticks
    }
}