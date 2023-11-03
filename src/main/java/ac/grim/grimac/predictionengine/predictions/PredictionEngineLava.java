package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.couldSkipTick && vector.isZeroPointZeroThree()) {
                double extraVelFromVertTickSkipUpwards = GrimMath.clamp(player.actualMovement.getY(), vector.vector.clone().getY(), vector.vector.clone().getY() + 0.05f);
                existingVelocities.add(new VectorData(vector.vector.clone().setY(extraVelFromVertTickSkipUpwards), vector, VectorData.VectorType.Jump));
            } else {
                existingVelocities.add(new VectorData(vector.vector.clone().add(new Vector(0, 0.04f, 0)), vector, VectorData.VectorType.Jump));
            }

            if (player.slightlyTouchingLava && player.lastOnGround && !player.onGround) {
                Vector withJump = vector.vector.clone();
                super.doJump(player, withJump);
                existingVelocities.add(new VectorData(withJump, vector, VectorData.VectorType.Jump));
            }
        }
    }
}
