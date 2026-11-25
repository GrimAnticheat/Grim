package ac.grim.grimac.predictionengine.predictions.input;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public interface Input {

    Vector3dm vector();

    Input normalize(GrimPlayer player);

    static Input createInput(GrimPlayer player, float sideways, float vertical, float forward) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new DoubleInput(sideways, vertical, forward);
        } else {
            return new FloatInput(sideways, vertical, forward);
        }
    }

}
