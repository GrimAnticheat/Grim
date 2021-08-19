package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.manager.init.Initable;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

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

    @Override
    public void start() {
        Field endOfTickList = Reflection.getField(NMSUtils.minecraftServerClass, List.class, 0);
        endOfTickList.setAccessible(true);
        try {
            Object end = Proxy.newProxyInstance(tickEnd.getClassLoader(),
                    new Class[]{tickEnd},
                    (proxy, method, args) -> {
                        //Bukkit.broadcastMessage("End of tick event!");
                        return null;
                    });
            ((List<Object>) endOfTickList.get(NMSUtils.getMinecraftServerInstance(Bukkit.getServer()))).add(end);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}