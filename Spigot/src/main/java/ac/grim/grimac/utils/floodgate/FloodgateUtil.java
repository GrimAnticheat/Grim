package ac.grim.grimac.utils.floodgate;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateUtil {

    private static boolean CHECKED_FOR_FLOODGATE;
    private static boolean FLOODGATE_PRESENT;

    public static boolean isFloodgatePlayer(UUID uuid) {
        if (!CHECKED_FOR_FLOODGATE) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                FLOODGATE_PRESENT = true;
            } catch (ClassNotFoundException e) {
                FLOODGATE_PRESENT = false;
            }
            CHECKED_FOR_FLOODGATE = true;
        }

        if (FLOODGATE_PRESENT) {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } else {
            return false;
        }
    }

}
