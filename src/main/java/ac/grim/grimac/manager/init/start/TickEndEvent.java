package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

public class TickEndEvent implements Initable {
    static Class<?> tickEnd = null;
    boolean hasTicked = true;

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

    @Override
    public void start() {
        Field endOfTickList = Reflection.getField(NMSUtils.minecraftServerClass, List.class, 0);
        endOfTickList.setAccessible(true);
        try {
            Object end = Proxy.newProxyInstance(tickEnd.getClassLoader(),
                    new Class[]{tickEnd},
                    (proxy, method, args) -> {
                        hasTicked = true;
                        tickRelMove();
                        return null;
                    });
            ((List<Object>) endOfTickList.get(NMSUtils.getMinecraftServerInstance(Bukkit.getServer()))).add(end);
        } catch (IllegalAccessException e) {
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
        }, 1, 1);
    }

    private void tickRelMove() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.checkManager.getReach().onEndOfTickEvent();
        }
    }
}