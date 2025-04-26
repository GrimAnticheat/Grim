package ac.grim.grimac.utils.reflection;

import org.geysermc.api.Geyser;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class GeyserUtil {
    // Floodgate is the authentication system for Geyser on servers that use Geyser
    // as a proxy instead of installing it as a plugin directly on the server
    private static boolean floodgate;
    private static boolean geyser;

    public static boolean isBedrockPlayer(UUID uuid) {
        return floodgate && FloodgateApi.getInstance().isFloodgatePlayer(uuid)
                || geyser && Geyser.api().isBedrockPlayer(uuid);
    }

    static {
        try {
            @SuppressWarnings("unused")
            Object obj = FloodgateApi.class;
            floodgate = true;
        } catch (NoClassDefFoundError e) {
            floodgate = false;
        }

        try {
            @SuppressWarnings("unused")
            Object obj = Geyser.class;
            geyser = true;
        } catch (NoClassDefFoundError e) {
            geyser = false;
        }
    }
}
