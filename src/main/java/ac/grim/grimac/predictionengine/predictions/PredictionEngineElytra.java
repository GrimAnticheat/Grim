package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Strangely, a player can jump while using an elytra
public class PredictionEngineElytra extends PredictionEngine {

    public static Vector getVectorForRotation(GrimPlayer player, float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = player.trigHandler.cos(f1);
        float f3 = player.trigHandler.sin(f1);
        float f4 = player.trigHandler.cos(f);
        float f5 = player.trigHandler.sin(f);
        return new Vector(f3 * f4, -f5, (double) (f2 * f4));
    }

    // Inputs have no effect on movement
    @Override
    public List<VectorData> multiplyPossibilitiesByInputs(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> results = new ArrayList<>();
        Vector currentLook = getVectorForRotation(player, player.yRot, player.xRot);

        for (VectorData data : possibleVectors) {
            data = data.setVector(handleFireworkOffset(player, data.vector.clone()), VectorData.VectorType.Firework);
            VectorData resultMovement = new VectorData(getElytraMovement(player, data.vector.clone(), currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99)), data, VectorData.VectorType.Elytra);
            results.add(resultMovement);
        }

        return results;
    }

    public Vector getElytraMovement(GrimPlayer player, Vector vector, Vector lookVector) {
        float yRotRadians = player.yRot * 0.017453292F;
        double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double d3 = vector.clone().setY(0).length();
        double d4 = lookVector.length();
        float f3 = player.trigHandler.cos(yRotRadians);
        f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
        vector.add(new Vector(0.0D, player.gravity * (-1.0D + (double) f3 * 0.75D), 0.0D));
        double d5;
        if (vector.getY() < 0.0D && d2 > 0.0D) {
            d5 = vector.getY() * -0.1D * (double) f3;
            vector.add(new Vector(lookVector.getX() * d5 / d2, d5, lookVector.getZ() * d5 / d2));
        }

        if (yRotRadians < 0.0F && d2 > 0.0D) {
            d5 = d3 * (double) (-player.trigHandler.sin(yRotRadians)) * 0.04D;
            vector.add(new Vector(-lookVector.getX() * d5 / d2, d5 * 3.2D, -lookVector.getZ() * d5 / d2));
        }

        if (d2 > 0) {
            vector.add(new Vector((lookVector.getX() / d2 * d3 - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - vector.getZ()) * 0.1D));
        }

        return vector;
    }

}
