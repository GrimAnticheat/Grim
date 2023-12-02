package ac.grim.grimac.utils.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class MultiLibUtil {

    public final static Method externalPlayerMethod = getMethod(Player.class, "isExternalPlayer");

    public static Method getMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    // TODO: cache external players for better performance, but this only matters for people using multi-lib
    public static boolean isExternalPlayer(Player player) {
        if (externalPlayerMethod == null || (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_18))) return false;
        try {
            return (boolean) externalPlayerMethod.invoke(player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
