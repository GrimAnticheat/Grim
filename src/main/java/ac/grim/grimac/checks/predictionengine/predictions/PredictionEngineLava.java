package ac.grim.grimac.checks.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {

    // Let shifting and holding space not be a false positive by allowing sneaking to override this
    // TODO: Do we have to apply this to other velocities


    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {

        for (VectorData vector : new HashSet<>(existingVelocities)) {
            // I don't believe you can ascend and jump regularly
            existingVelocities.add(new VectorData(vector.vector.clone().add(new Vector(0, 0.04, 0)), vector.vectorType));
            Vector withJump = vector.vector.clone();
            super.doJump(player, withJump);
            existingVelocities.add(new VectorData(withJump, vector.vectorType));
        }

        //handleSwimJump(grimPlayer, grimPlayer.clientVelocity);
        //super.addJumpIfNeeded(grimPlayer);
    }
}
