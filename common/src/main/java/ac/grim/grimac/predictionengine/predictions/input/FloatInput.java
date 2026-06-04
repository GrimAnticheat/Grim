package ac.grim.grimac.predictionengine.predictions.input;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;

public record FloatInput(float sideways, float vertical, float forward) implements Input {
    @Override
    public Vector3dm vector() {
        return new Vector3dm(sideways, vertical, forward);
    }

    @Override
    public Input normalize(GrimPlayer player) {
        // this does nothing because of the way input is later used in FloatInputTransformer#getMovementResultFromInput
        // in 1.13 and below the result is calculated based on clean transformed input and later normalized
        // while in 1.14+ the input can be normalized earlier because getMovementResultFromInput does not depend on the clean input
        return this;
    }
}
