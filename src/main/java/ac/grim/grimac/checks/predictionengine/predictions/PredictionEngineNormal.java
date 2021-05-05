package ac.grim.grimac.checks.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {
    public static final Material scaffolding = XMaterial.SCAFFOLDING.parseMaterial();

    public static void staticVectorEndOfTick(GrimPlayer grimPlayer, Vector vector) {
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

    @Override
    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        if (grimPlayer.isClimbing) {
            vector.setX(Mth.clamp(vector.getX(), -0.15, 0.15));
            vector.setZ(Mth.clamp(vector.getZ(), -0.15, 0.15));
            vector.setY(Math.max(vector.getY(), -0.15));

            // Yes, this uses shifting not crouching
            if (vector.getY() < 0.0 && !(ChunkCache.getBukkitBlockDataAt(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ).getMaterial() == scaffolding) && grimPlayer.isSneaking && !grimPlayer.specialFlying) {
                vector.setY(0.0);
            }
        }

        return vector;
    }

    @Override
    public Set<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        Set<Vector> regularInputs = super.fetchPossibleInputs(grimPlayer);

        // This is WRONG! Vanilla has this system at the end
        // However, due to 1.9 reduced movement precision, we aren't informed that the player could have this velocity
        // We still do climbing at the end, as it uses a different client velocity
        if (grimPlayer.isClimbing) {
            Vector hackyClimbVector = grimPlayer.clientVelocity.clone().setY(0.2);
            staticVectorEndOfTick(grimPlayer, hackyClimbVector);
            regularInputs.add(hackyClimbVector);
        }

        return regularInputs;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double d, float friction) {
        grimPlayer.clientVelocityOnLadder = null;

        if (grimPlayer.isClimbing) {
            grimPlayer.clientVelocityOnLadder = grimPlayer.clientVelocity.clone().setY(0.2);
        }

        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(grimPlayer, vector);
        }

        super.endOfTick(grimPlayer, d, friction);
    }
}
