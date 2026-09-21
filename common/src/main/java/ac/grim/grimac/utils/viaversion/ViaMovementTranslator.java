package ac.grim.grimac.utils.viaversion;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath;
import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath;
import com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta;
import com.github.retrooper.packetevents.protocol.vector.vecdelta.SteppedVecDelta;
import com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.List;

public final class ViaMovementTranslator {

    private static final int INTERPOLATION_STEP_TICKS = 3;

    public static VecDelta convert(ClientVersion clientVersion, ServerVersion serverVersion, VecDelta delta) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_26_3) && serverVersion.isOlderThan(ServerVersion.V_26_3)) {
            return legacyToModern((LinearVecDelta) delta);
        } else if (clientVersion.isOlderThan(ClientVersion.V_26_3) && serverVersion.isNewerThanOrEquals(ServerVersion.V_26_3)) {
            return modernToLegacy(delta);
        }

        return delta;
    }

    public static PositionPath convert(ClientVersion clientVersion, ServerVersion serverVersion, PositionPath path) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_26_3) && serverVersion.isOlderThan(ServerVersion.V_26_3)) {
            return legacyToModern(path);
        } else if (clientVersion.isOlderThan(ClientVersion.V_26_3) && serverVersion.isNewerThanOrEquals(ServerVersion.V_26_3)) {
            return modernToLegacy(path);
        }

        return path;
    }

    public static SteppedVecDelta legacyToModern(LinearVecDelta delta) {
        return new SteppedVecDelta(List.of(new SteppedVecDelta.DeltaStep(INTERPOLATION_STEP_TICKS, delta.dxRaw(), delta.dyRaw(), delta.dzRaw())));
    }

    public static LinearVecDelta modernToLegacy(VecDelta delta) {
        if (delta instanceof LinearVecDelta linear) {
            return linear;
        }

        int deltaX = 0;
        int deltaY = 0;
        int deltaZ = 0;
        for (SteppedVecDelta.DeltaStep step : ((SteppedVecDelta) delta).steps()) {
            deltaX += step.dxRaw();
            deltaY += step.dyRaw();
            deltaZ += step.dzRaw();
        }

        return new LinearVecDelta((short) deltaX, (short) deltaY, (short) deltaZ);
    }

    public static SteppedPositionPath legacyToModern(PositionPath path) {
        return new SteppedPositionPath(List.of(new SteppedPositionPath.Step(path.getEndPosition(), INTERPOLATION_STEP_TICKS)));
    }

    public static PositionPath modernToLegacy(PositionPath path) {
        if (path instanceof SteppedPositionPath stepped) {
            Vector3d position = stepped.getSteps().isEmpty() ? Vector3d.zero() : stepped.getEndPosition();
            return new LinearPositionPath(position);
        }

        return path;
    }

}
