package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.checks.movement.MovementVelocityCheck;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PredictionEngineNormal extends PredictionEngine {
    @Override
    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        if (grimPlayer.lastClimbing) {
            vector.setX(Mth.clamp(vector.getX(), -0.15, 0.15));
            vector.setZ(Mth.clamp(vector.getZ(), -0.15, 0.15));
            vector.setY(Math.max(vector.getY(), -0.15));

            if (vector.getY() < 0.0 && !grimPlayer.bukkitPlayer.getWorld().getBlockAt(grimPlayer.bukkitPlayer.getLocation()).getType().equals(Material.SCAFFOLDING) && grimPlayer.bukkitPlayer.isSneaking()) {
                vector.setY(0.0);
            }
        }

        return vector;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double d, float f6) {
        grimPlayer.clientVelocityOnLadder = null;
        if (grimPlayer.lastClimbing) {
            grimPlayer.clientVelocityOnLadder = grimPlayer.clientVelocity.clone().setY(0.2);
        }

        //Vector vector = grimPlayer.clientVelocity;

        //for (Vector vector : Collections.singletonList(grimPlayer.clientVelocity)) {
        //Bukkit.broadcastMessage("Vector (old) " + grimPlayer.clientVelocity);

        grimPlayer.clientVelocity = MovementVelocityCheck.move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity);

        // Okay, this seems to just be gravity stuff
        double d9 = grimPlayer.clientVelocity.getY();
        if (grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.LEVITATION)) {
            d9 += (0.05 * (double) (grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1) - grimPlayer.clientVelocity.getY()) * 0.2;
            //this.fallDistance = 0.0f;
        } else if (grimPlayer.bukkitPlayer.getLocation().isChunkLoaded()) {
            if (grimPlayer.bukkitPlayer.hasGravity()) {
                d9 -= d;
            }
        } else {
            d9 = grimPlayer.clientVelocity.getY() > 0.0 ? -0.1 : 0.0;
        }

        grimPlayer.clientVelocity.setX(grimPlayer.clientVelocity.getX() * f6);
        grimPlayer.clientVelocity.setY(d9 * 0.9800000190734863);
        grimPlayer.clientVelocity.setZ(grimPlayer.clientVelocity.getZ() * f6);
        //}

        //for (Vector vector : Collections.singletonList(grimPlayer.clientVelocity)) {
        //Bukkit.broadcastMessage("Vector (new) " + grimPlayer.clientVelocity);
        //}
    }
}
