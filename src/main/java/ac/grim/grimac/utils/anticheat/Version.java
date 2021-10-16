package ac.grim.grimac.utils.anticheat;

import io.github.retrooper.packetevents.utils.server.ServerVersion;

public class Version {
    private static final boolean isFlat = ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13);

    public static boolean isFlat() {
        return isFlat;
    }
}
