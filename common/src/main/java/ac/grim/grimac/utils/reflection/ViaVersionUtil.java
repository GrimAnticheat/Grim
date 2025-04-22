package ac.grim.grimac.utils.reflection;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.PacketEvents;


enum ViaState {
    UNKNOWN,
    DISABLED,
    ENABLED
}

public class ViaVersionUtil {
    private static ViaState available = ViaState.UNKNOWN;
    private static boolean isViaLoaded = false;

    private ViaVersionUtil() {
    }

    private static void load() {
        ClassLoader classLoader = PacketEvents.getAPI().getPlugin().getClass().getClassLoader();
        try {
            classLoader.loadClass("com.viaversion.viaversion.api.Via");
            isViaLoaded = true;
        } catch (Exception e) {
            try {
                classLoader.loadClass("us.myles.ViaVersion.api.Via");
                isViaLoaded = true;
            } catch (ClassNotFoundException ex) {
                isViaLoaded = false; // unnecessary code, but it makes the purpose of load() more clear
            }
        }
    }

    public static void checkIfViaIsPresent() {
        boolean present = GrimAPI.INSTANCE.getPluginManager().isPluginEnabled("ViaVersion");
        available = present ? ViaState.ENABLED : ViaState.DISABLED;
    }

    public static boolean isAvailable() {
        if (available == ViaState.UNKNOWN) { // Plugins haven't loaded... let's refer to whether we have a class
            return getViaVersionAccessor();
        }
        return available == ViaState.ENABLED;
    }

    public static boolean getViaVersionAccessor() {
        load();
        return isViaLoaded;
    }
}
