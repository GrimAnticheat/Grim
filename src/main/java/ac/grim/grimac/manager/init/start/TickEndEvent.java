package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.lists.HookedListWrapper;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.Bukkit;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

// Copied from: https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/inject/ServerInjector.java
@SuppressWarnings(value = {"unchecked", "deprecated"})
public class TickEndEvent implements Initable {
    boolean hasTicked = true;

    private static void tickRelMove() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.checkManager.getEntityReplication().onEndOfTickEvent();
        }
    }

    @Override
    public void start() {
        // Inject so we can add the final transaction pre-flush event
        try {
            Object connection = NMSUtils.getMinecraftServerConnection();

            Field connectionsList = Reflection.getField(connection.getClass(), List.class, 1);
            List<Object> endOfTickObject = (List<Object>) connectionsList.get(connection);

            // Use a list wrapper to check when the size method is called
            // Unsure why synchronized is needed because the object itself gets synchronized
            // but whatever.  At least plugins can't break it, I guess.
            //
            // Pledge injects into another list, so we should be safe injecting into this one
            List<?> wrapper = Collections.synchronizedList(new HookedListWrapper<Object>(endOfTickObject) {
                @Override
                public void onIterator() {
                    hasTicked = true;
                    tickRelMove();
                }
            });

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObject(connection, unsafe.objectFieldOffset(connectionsList), wrapper);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // This should NEVER happen!  But there are two scenarios where it could:
        // 1) Some stupid jar messed up our reflection
        // 2) Some stupid jar doesn't tick the list at the end for "optimization"
        // 3) Some stupid jar removed the list at the end because it wasn't needed
        // 4) Someone else injected after our delayed injection (which tries to not overwrite Pledge)
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