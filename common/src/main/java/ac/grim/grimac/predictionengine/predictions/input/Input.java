package ac.grim.grimac.predictionengine.predictions.input;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;

public interface Input {
    Vector3dm vector();
    Input normalize(GrimPlayer player);
}
