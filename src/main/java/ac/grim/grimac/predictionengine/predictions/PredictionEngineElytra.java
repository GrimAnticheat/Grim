package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.VectorUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

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
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> results = new ObjectArrayList<>();
        Vector currentLook = getVectorForRotation(player, player.yRot, player.xRot);
        Vector lastLook = getVectorForRotation(player, player.lastYRot, player.lastXRot);

        int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;

        for (VectorData data : possibleVectors) {
            Vector boostOne = data.vector.clone();
            Vector boostTwo = data.vector.clone();

            Vector fireworksResult = getElytraMovement(player, boostOne.clone(), currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));

            if (maxFireworks > 0) {
                for (int i = 0; i < maxFireworks; i++) {
                    boostOne.add(new Vector(currentLook.getX() * 0.1 + (currentLook.getX() * 1.5 - boostOne.getX()) * 0.5, currentLook.getY() * 0.1 + (currentLook.getY() * 1.5 - boostOne.getY()) * 0.5, (currentLook.getZ() * 0.1 + (currentLook.getZ() * 1.5 - boostOne.getZ()) * 0.5)));
                    boostTwo.add(new Vector(lastLook.getX() * 0.1 + (lastLook.getX() * 1.5 - boostTwo.getX()) * 0.5, lastLook.getY() * 0.1 + (lastLook.getY() * 1.5 - boostTwo.getY()) * 0.5, (lastLook.getZ() * 0.1 + (lastLook.getZ() * 1.5 - boostTwo.getZ()) * 0.5)));
                }

                getElytraMovement(player, boostOne, currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));
                getElytraMovement(player, boostTwo, currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));

                Vector cutOne = VectorUtils.cutBoxToVector(player.actualMovement, boostOne, fireworksResult);
                Vector cutTwo = VectorUtils.cutBoxToVector(player.actualMovement, boostTwo, fireworksResult);
                fireworksResult = VectorUtils.cutBoxToVector(player.actualMovement, cutOne, cutTwo);
            }

            results.add(data.returnNewModified(fireworksResult, VectorData.VectorType.Elytra));
        }

        return results;
    }

    public Vector getElytraMovement(GrimPlayer player, Vector vector, Vector lookVector) {
        float yRotRadians = player.yRot * 0.017453292F;
        double horizontalSqrt = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double horizontalLength = vector.clone().setY(0).length();
        double length = lookVector.length();
        float vertCosRotation = player.trigHandler.cos(yRotRadians);
        vertCosRotation = (float) ((double) vertCosRotation * (double) vertCosRotation * Math.min(1.0D, length / 0.4D));
        vector.add(new Vector(0.0D, player.gravity * (-1.0D + (double) vertCosRotation * 0.75D), 0.0D));
        double d5;

        // Handle slowing the player down when falling
        if (vector.getY() < 0.0D && horizontalSqrt > 0.0D) {
            d5 = vector.getY() * -0.1D * (double) vertCosRotation;
            vector.add(new Vector(lookVector.getX() * d5 / horizontalSqrt, d5, lookVector.getZ() * d5 / horizontalSqrt));
        }

        // Handle accelerating the player when they are looking down
        if (yRotRadians < 0.0F && horizontalSqrt > 0.0D) {
            d5 = horizontalLength * (double) (-player.trigHandler.sin(yRotRadians)) * 0.04D;
            vector.add(new Vector(-lookVector.getX() * d5 / horizontalSqrt, d5 * 3.2D, -lookVector.getZ() * d5 / horizontalSqrt));
        }

        // Handle accelerating the player sideways
        if (horizontalSqrt > 0) {
            vector.add(new Vector((lookVector.getX() / horizontalSqrt * horizontalLength - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / horizontalSqrt * horizontalLength - vector.getZ()) * 0.1D));
        }

        return vector;
    }

    // Yes... you can jump while using an elytra as long as you are on the ground
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        new PredictionEngineNormal().addJumpsToPossibilities(player, existingVelocities);
    }

    @Override
    public Vector handleFireworkMovementLenience(GrimPlayer player, Vector vector) {
        return vector;
    }
}
