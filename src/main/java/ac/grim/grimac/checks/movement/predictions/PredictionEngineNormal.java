package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
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
            vectorEndOfTick(grimPlayer, hackyClimbVector);
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

            if (vector.getY() < 0.0 && !(ChunkCache.getBlockDataAt(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ).getBlock() instanceof BlockScaffolding) && grimPlayer.wasSneaking && !grimPlayer.isFlying) {
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
            vectorEndOfTick(grimPlayer, vector);
        }

        super.endOfTick(grimPlayer, d, friction);
    }

    public void vectorEndOfTick(GrimPlayer grimPlayer, Vector vector) {
        double d9 = vector.getY();
        if (grimPlayer.levitationAmplifier > 0) {
            d9 += (0.05 * (double) (grimPlayer.levitationAmplifier + 1) - vector.getY()) * 0.2;
        } else if (ChunkCache.getChunk((int) grimPlayer.x >> 4, (int) grimPlayer.z >> 4) != null) {
            // Commenting out hasGravity check because playesr always have gravity
            d9 -= grimPlayer.gravity;
        } else {
            d9 = vector.getY() > 0.0 ? -0.1 : 0.0;
        }

        vector.setX(vector.getX() * grimPlayer.friction);
        vector.setY(d9 * 0.9800000190734863);
        vector.setZ(vector.getZ() * grimPlayer.friction);
    }
}
