package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictionEngineElytra extends PredictionEngine {

    // Inputs have no effect on movement
    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> results = new ArrayList<>();
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

                Vector cutOne = cutVectorsToPlayerMovement(player.actualMovement, boostOne, fireworksResult);
                Vector cutTwo = cutVectorsToPlayerMovement(player.actualMovement, boostTwo, fireworksResult);
                fireworksResult = cutVectorsToPlayerMovement(player.actualMovement, cutOne, cutTwo);
            }

            data = data.setVector(fireworksResult, VectorData.VectorType.Elytra);
            results.add(data);
        }

        return results;
    }

    public static Vector getVectorForRotation(GrimPlayer player, float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = player.trigHandler.cos(f1);
        float f3 = player.trigHandler.sin(f1);
        float f4 = player.trigHandler.cos(f);
        float f5 = player.trigHandler.sin(f);
        return new Vector(f3 * f4, -f5, (double) (f2 * f4));
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

    public static Vector cutVectorsToPlayerMovement(Vector vectorToCutTo, Vector vectorOne, Vector vectorTwo) {
        double xMin = Math.min(vectorOne.getX(), vectorTwo.getX());
        double xMax = Math.max(vectorOne.getX(), vectorTwo.getX());
        double yMin = Math.min(vectorOne.getY(), vectorTwo.getY());
        double yMax = Math.max(vectorOne.getY(), vectorTwo.getY());
        double zMin = Math.min(vectorOne.getZ(), vectorTwo.getZ());
        double zMax = Math.max(vectorOne.getZ(), vectorTwo.getZ());

        Vector cutCloned = vectorToCutTo.clone();

        if (xMin > vectorToCutTo.getX() || xMax < vectorToCutTo.getX()) {
            if (Math.abs(vectorToCutTo.getX() - xMin) < Math.abs(vectorToCutTo.getX() - xMax)) {
                cutCloned.setX(xMin);
            } else {
                cutCloned.setX(xMax);
            }
        }

        if (yMin > vectorToCutTo.getY() || yMax < vectorToCutTo.getY()) {
            if (Math.abs(vectorToCutTo.getY() - yMin) < Math.abs(vectorToCutTo.getY() - yMax)) {
                cutCloned.setY(yMin);
            } else {
                cutCloned.setY(yMax);
            }
        }

        if (zMin > vectorToCutTo.getZ() || zMax < vectorToCutTo.getZ()) {
            if (Math.abs(vectorToCutTo.getZ() - zMin) < Math.abs(vectorToCutTo.getZ() - zMax)) {
                cutCloned.setZ(zMin);
            } else {
                cutCloned.setZ(zMax);
            }
        }

        return cutCloned;
    }

    // Yes... you can jump while using an elytra as long as you are on the ground
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        if (!player.lastOnGround || player.onGround && !(player.uncertaintyHandler.lastPacketWasGroundPacket && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree))
            return;

        for (VectorData vector : new HashSet<>(existingVelocities)) {
            Vector jump = vector.vector.clone();
            JumpPower.jumpFromGround(player, jump);

            existingVelocities.add(new VectorData(jump, VectorData.VectorType.Jump));
        }
    }

    @Override
    public Vector handleFireworkMovementLenience(GrimPlayer player, Vector vector) {
        return vector;
    }
}
