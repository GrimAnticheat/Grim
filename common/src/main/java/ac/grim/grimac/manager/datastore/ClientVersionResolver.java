package ac.grim.grimac.manager.datastore;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Maps legacy client-version release-name strings (e.g. {@code "1.21.1"}) to
 * PacketEvents protocol-version numbers. Lives in the plugin module because
 * PacketEvents is only on the classpath here — the migrator in the shared
 * storage module accepts a generic {@code Function<String, Integer>} so it
 * stays PacketEvents-free and portable.
 * <p>
 * Returns {@code -1} when the string doesn't resolve to a known
 * {@link ClientVersion} — unknown / pre-1.8 / freshly-released / bogus.
 */
public final class ClientVersionResolver {

    private ClientVersionResolver() {}

    public static int legacyStringToPvn(@Nullable String versionString) {
        if (versionString == null) return -1;
        String trimmed = versionString.trim();
        if (trimmed.isEmpty()) return -1;
        // Reverse the PE release-name algorithm: enum is V_<major>_<minor>[_<patch>]
        // where dots become underscores. "1.21.1" → "V_1_21_1".
        String enumName = "V_" + trimmed.replace('.', '_').toUpperCase(Locale.ROOT);
        try {
            ClientVersion cv = ClientVersion.valueOf(enumName);
            return cv.getProtocolVersion();
        } catch (IllegalArgumentException notFound) {
            return -1;
        }
    }
}
