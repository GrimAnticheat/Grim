package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;

public class NoFallChecker extends PostPredictionCheck {
    public NoFallChecker(GrimPlayer player) {
        super(player);
    }
}
