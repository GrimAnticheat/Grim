package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {

    // Let shifting and holding space not be a false positive by allowing sneaking to override this
    // TODO: Do we have to apply this to other velocities


    @Override
    public void addJumpsToPossibilities(GrimPlayer grimPlayer, Set<Vector> existingVelocities) {

        for (Vector vector : new HashSet<>(existingVelocities)) {
            // I don't believe you can ascend and jump regularly
            existingVelocities.add(vector.add(new Vector(0, 0.04, 0)));
            Vector withJump = vector.clone();
            super.doJump(grimPlayer, withJump);
            existingVelocities.add(withJump);
        }

        //handleSwimJump(grimPlayer, grimPlayer.clientVelocity);
        //super.addJumpIfNeeded(grimPlayer);
    }
}
