package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.util.Vector;

public class PredictionEngine {
    public Vector guessBestMovement(float f, GrimPlayer grimPlayer) {
        double bestInput = Double.MAX_VALUE;

        for (Vector possibleLastTickOutput : grimPlayer.getPossibleVelocities()) {
            Vector theoreticalInput = getBestTheoreticalPlayerInput(grimPlayer.actualMovement.clone().subtract(possibleLastTickOutput), f, grimPlayer.xRot);
            Vector possibleInput = getBestPossiblePlayerInput(grimPlayer.isSneaking, theoreticalInput);

            double resultAccuracy = theoreticalInput.distance(possibleInput);
            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.bestOutput = possibleLastTickOutput;
                grimPlayer.theoreticalInput = theoreticalInput;
                grimPlayer.possibleInput = possibleInput;
            }
        }

        return grimPlayer.bestOutput.clone().add(getMovementResultFromInput(grimPlayer.possibleInput, f, grimPlayer.xRot));
    }

    // These math equations are based off of the vanilla equations, made impossible to divide by 0
    public static Vector getBestTheoreticalPlayerInput(Vector wantedMovement, float f, float f2) {
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public static Vector getBestPossiblePlayerInput(boolean isSneaking, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        if (isSneaking) {
            bestPossibleX = Math.min(Math.max(-1, Math.round(theoreticalInput.getX() / 0.3)), 1) * 0.3f;
            bestPossibleZ = Math.min(Math.max(-1, Math.round(theoreticalInput.getZ() / 0.3)), 1) * 0.3f;
        } else {
            bestPossibleX = Math.min(Math.max(-1, Math.round(theoreticalInput.getX())), 1);
            bestPossibleZ = Math.min(Math.max(-1, Math.round(theoreticalInput.getZ())), 1);
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);

        inputVector.multiply(0.98);
        if (inputVector.lengthSquared() > 1) inputVector.normalize();

        return inputVector;
    }

    // This is just the vanilla equation, which accepts invalid inputs greater than 1
    public static Vector getMovementResultFromInput(Vector inputVector, float f, float f2) {
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);

        double xResult = inputVector.getX() * f4 - inputVector.getZ() * f3;
        double zResult = inputVector.getZ() * f4 + inputVector.getX() * f3;

        return new Vector(xResult * f, 0, zResult * f);
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        return vector;
    }
}
