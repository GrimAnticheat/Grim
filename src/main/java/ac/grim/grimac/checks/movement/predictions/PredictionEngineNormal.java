package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.movementTick.MovementVelocityCheck;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import net.minecraft.server.v1_16_R3.BlockScaffolding;
import org.bukkit.util.Vector;

import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {

    @Override
    public Set<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        Set<Vector> regularInputs = super.fetchPossibleInputs(grimPlayer);

        // This is WRONG! Vanilla has this system at the end
        // However, due to 1.9 reduced movement precision, we aren't informed that the player could have this velocity
        // We still do climbing at the end, as it uses a different client velocity
        if (grimPlayer.isClimbing) {
            Vector hackyClimbVector = grimPlayer.clientVelocity.clone().setY(0.2);
            MovementVelocityCheck.vectorEndOfTick(grimPlayer, hackyClimbVector);
            regularInputs.add(hackyClimbVector);
        }

        return regularInputs;
    }

    @Override
    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        if (grimPlayer.isClimbing) {
            vector.setX(Mth.clamp(vector.getX(), -0.15, 0.15));
            vector.setZ(Mth.clamp(vector.getZ(), -0.15, 0.15));
            vector.setY(Math.max(vector.getY(), -0.15));

            // Yes, this uses shifting not crouching
            if (vector.getY() < 0.0 && !(ChunkCache.getBlockDataAt(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ).getBlock() instanceof BlockScaffolding) && grimPlayer.isSneaking && !grimPlayer.specialFlying) {
                vector.setY(0.0);
            }
        }

        return vector;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double d, float friction) {
        grimPlayer.clientVelocityOnLadder = null;

        if (grimPlayer.isClimbing) {
            grimPlayer.clientVelocityOnLadder = grimPlayer.clientVelocity.clone().setY(0.2);
        }

        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            MovementVelocityCheck.vectorEndOfTick(grimPlayer, vector);
        }

        super.endOfTick(grimPlayer, d, friction);
    }
}
