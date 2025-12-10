package ac.grim.grimac.checks.impl.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.util.LpVector3d;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VectorPrecisionConverter {

    private static final ServerVersion SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion();

    public static Vector3d convert(ClientVersion version, Vector3d vector) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_21_9) && SERVER_VERSION.isOlderThanOrEquals(ServerVersion.V_1_21_8)) {
            return VectorPrecisionConverter.legacyToLp(vector);
        } else if (version.isOlderThanOrEquals(ClientVersion.V_1_21_7) && SERVER_VERSION.isNewerThanOrEquals(ServerVersion.V_1_21_9)) {
            return VectorPrecisionConverter.lpToLegacy(vector);
        }

        return vector;
    }

    public static Vector3d legacyToLp(Vector3d legacy) {
        PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(Unpooled.buffer());
        LpVector3d.write(wrapper, legacy);
        return LpVector3d.read(wrapper);
    }

    private static final double PRECISION_LOSS_FIX = 1e-11d;

    public static Vector3d lpToLegacy(Vector3d lp) {
        int xi = (int) (lp.x * 8000d + Math.copySign(PRECISION_LOSS_FIX, lp.x));
        int yi = (int) (lp.y * 8000d + Math.copySign(PRECISION_LOSS_FIX, lp.y));
        int zi = (int) (lp.z * 8000d + Math.copySign(PRECISION_LOSS_FIX, lp.z));

        short x = (short) xi;
        short y = (short) yi;
        short z = (short) zi;

        return new Vector3d(
                x / 8000d,
                y / 8000d,
                z / 8000d
        );
    }

}
