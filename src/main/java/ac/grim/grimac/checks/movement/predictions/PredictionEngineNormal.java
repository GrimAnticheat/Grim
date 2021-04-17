package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import net.minecraft.server.v1_16_R3.BlockScaffolding;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PredictionEngineNormal extends PredictionEngine {
    @Override
    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        if (grimPlayer.lastClimbing) {
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
        if (grimPlayer.lastClimbing) {
            grimPlayer.clientVelocityOnLadder = grimPlayer.clientVelocity.clone().setY(0.2);
        }

        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            double d9 = vector.getY();
            if (grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.LEVITATION)) {
                d9 += (0.05 * (double) (grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1) - vector.getY()) * 0.2;
            } else if (ChunkCache.getChunk((int) grimPlayer.x >> 4, (int) grimPlayer.z >> 4) != null) {
                // Commenting out hasGravity check because playesr always have gravity
                d9 -= d;
            } else {
                d9 = vector.getY() > 0.0 ? -0.1 : 0.0;
            }

            vector.setX(vector.getX() * friction);
            vector.setY(d9 * 0.9800000190734863);
            vector.setZ(vector.getZ() * friction);
        }
    }
}
