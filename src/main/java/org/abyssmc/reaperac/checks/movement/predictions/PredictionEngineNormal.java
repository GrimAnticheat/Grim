package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.Material;
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
}
