package ac.grim.grimac.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ViaVersionUtil {
    private static final boolean isViaLoaded;

    static {
        ClassLoader classLoader = PacketEvents.getAPI().getPlugin().getClass().getClassLoader();
        boolean temp;
        try {
            classLoader.loadClass("com.viaversion.viaversion.api.Via");
            temp = true;
        } catch (Exception e) {
            try {
                classLoader.loadClass("us.myles.ViaVersion.api.Via");
                LogUtil.error("Using unsupported ViaVersion 4.0 API, update ViaVersion to 5.0");
                temp = false;
            } catch (ClassNotFoundException ex) {
                temp = false;
            }
        }
        isViaLoaded = temp;
    }

    public static boolean isAvailable() {
        return isViaLoaded;
    }
}
