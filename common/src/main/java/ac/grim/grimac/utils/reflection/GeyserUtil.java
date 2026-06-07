package ac.grim.grimac.utils.reflection;

import lombok.experimental.UtilityClass;
import org.geysermc.api.Geyser;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@UtilityClass
public class GeyserUtil {
    // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
    private static final boolean floodgate = ReflectionUtils.hasClass("org.geysermc.floodgate.api.FloodgateApi");
    private static final boolean geyser = ReflectionUtils.hasClass("org.geysermc.api.Geyser");

    public static boolean isBedrockPlayer(UUID uuid) {
        return floodgate && FloodgateApi.getInstance().isFloodgatePlayer(uuid)
                || geyser && Geyser.api().isBedrockPlayer(uuid);
    }

    // Geyser/Floodgate prefixes Bedrock usernames (default prefix is ".") so they
    // don't collide with Java usernames. Detecting that prefix lets us treat the
    // player as Bedrock even when the Geyser/Floodgate API isn't reachable from here.
    public static boolean isBedrockName(@Nullable String name) {
        return name != null && name.startsWith(".");
    }
}
