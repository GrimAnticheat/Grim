package ac.grim.grimac.predictionengine.predictions.input;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.input.impl.DoubleInputTransformer;
import ac.grim.grimac.predictionengine.predictions.input.impl.FloatInputTransformer;
import ac.grim.grimac.predictionengine.predictions.input.impl.ModernInputTransformer;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public interface InputTransformer<INPUT extends Input> {

    FloatInputTransformer FLOAT_INPUT_TRANSFORMER = new FloatInputTransformer();
    DoubleInputTransformer DOUBLE_INPUT_TRANSFORMER = new DoubleInputTransformer();
    ModernInputTransformer MODERN_INPUT_TRANSFORMER = new ModernInputTransformer();

    INPUT transformInputsToVector(GrimPlayer player, int sideways, int vertical, int forward);

    Vector3dm getMovementResultFromInput(GrimPlayer player, Input inputVector, float speed, float yaw);

    static InputTransformer<?> getTransformer(GrimPlayer player) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            return FLOAT_INPUT_TRANSFORMER;
        }

        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5) ? MODERN_INPUT_TRANSFORMER : DOUBLE_INPUT_TRANSFORMER;
    }

}
