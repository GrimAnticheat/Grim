package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        final VectorData[] vectorsToProcess = existingVelocities.toArray(new VectorData[0]);
        for (VectorData vector : vectorsToProcess) {
            final double startX = vector.vectorX;
            final double startY = vector.vectorY;
            final double startZ = vector.vectorZ;

            if (player.couldSkipTick && vector.isZeroPointZeroThree()) {
                double extraVelFromVertTickSkipUpwards = GrimMath.clamp(player.actualMovement.getY(), startY, startY + 0.05f);
                existingVelocities.add(new VectorData(startX, extraVelFromVertTickSkipUpwards, startZ, vector, VectorData.VectorType.Jump));
            } else {
                existingVelocities.add(new VectorData(startX, startY + 0.04f, startZ, vector, VectorData.VectorType.Jump));
            }

            if (player.slightlyTouchingLava && player.lastOnGround && !player.onGround) {
                double[] jumpValues = {startX, startY, startZ};
                super.doJump(player, jumpValues);
                existingVelocities.add(new VectorData(jumpValues[0], jumpValues[1], jumpValues[2], vector, VectorData.VectorType.Jump));
            }
        }
    }
}
