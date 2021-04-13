package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.util.Vector;

public class PredictionEngineLava extends PredictionEngine {
    @Override
    public void addJumpIfNeeded(GrimPlayer grimPlayer) {
        grimPlayer.clientVelocityJumping = grimPlayer.clientVelocity.clone().add(new Vector(0, 0.04, 0));
        handleSwimJump(grimPlayer, grimPlayer.clientVelocity);
        //super.addJumpIfNeeded(grimPlayer);
    }
}
