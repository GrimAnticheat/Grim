package ac.grim.grimac.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.reflection.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class GeyserUtil {
    private static boolean CHECKED_FOR_GEYSER = false;
    private static boolean GEYSER_PRESENT = false;
    private static Class<?> GEYSER_CLASS;
    private static Class<?> GEYSER_API_CLASS;
    private static Method GEYSER_API_METHOD;
    private static Method CONNECTION_BY_UUID_METHOD;

    public static boolean isGeyserPlayer(UUID uuid) {
        if (!CHECKED_FOR_GEYSER) {
            try {
                ClassLoader classLoader = PacketEvents.getAPI().getPlugin().getClass().getClassLoader();
                GEYSER_CLASS = classLoader.loadClass("org.geysermc.api.Geyser");
                GEYSER_PRESENT = true;
            } catch (ClassNotFoundException e) {
                GEYSER_PRESENT = false;
            }
            CHECKED_FOR_GEYSER = true;
        }

        if (GEYSER_PRESENT) {
            if (GEYSER_API_CLASS == null) {
                try {
                    ClassLoader classLoader = PacketEvents.getAPI().getPlugin().getClass().getClassLoader();
                    GEYSER_API_CLASS = classLoader.loadClass("org.geysermc.api.GeyserApiBase");
                } catch (ClassNotFoundException e) {
                    LogUtil.error("Failed to load GeyserApiBase class", e);
                }
            }
            if (GEYSER_API_METHOD == null) {
                GEYSER_API_METHOD = Reflection.getMethodExact(GEYSER_CLASS, "api", null);
            }
            if (CONNECTION_BY_UUID_METHOD == null) {
                CONNECTION_BY_UUID_METHOD = Reflection.getMethod(GEYSER_API_CLASS, "connectionByUuid", 0);
            }
            Object apiInstance = null;
            try {
                apiInstance = GEYSER_API_METHOD.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LogUtil.error("Failed to invoke GeyserApiBase.api()", e);
            }
            Object connection = null;
            try {
                if (apiInstance != null) {
                    connection = CONNECTION_BY_UUID_METHOD.invoke(apiInstance, uuid);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                LogUtil.error("Failed to invoke GeyserApiBase.connectionByUuid()", e);
            }
            return connection != null;
        }
        return false;
    }
}
