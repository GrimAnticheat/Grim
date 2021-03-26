package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.JumpPower;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

public abstract class PredictionEngine {
    public Vector guessBestMovement(float f, GrimPlayer grimPlayer, boolean collision) {
        double bestInput = Double.MAX_VALUE;
        addJumpIfNeeded(grimPlayer);

        for (Vector possibleLastTickOutput : grimPlayer.getPossibleVelocities()) {
            // This method clamps climbing velocity (as in vanilla), if needed.
            possibleLastTickOutput = handleOnClimbable(possibleLastTickOutput, grimPlayer);

            Vector theoreticalInput = getBestTheoreticalPlayerInput(grimPlayer.actualMovement.clone().subtract(possibleLastTickOutput), f, grimPlayer.xRot);
            Vector possibleInput = getBestPossiblePlayerInput(grimPlayer.isSneaking, theoreticalInput);
            Vector possibleInputVelocityResult = possibleLastTickOutput.clone().add(getMovementResultFromInput(possibleInput, f, grimPlayer.xRot));

            double resultAccuracy = possibleInputVelocityResult.distanceSquared(grimPlayer.actualMovement);

            Bukkit.broadcastMessage("Accuracy for " + possibleInputVelocityResult + " " + resultAccuracy);
            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.bestOutput = possibleLastTickOutput;
                grimPlayer.theoreticalInput = theoreticalInput;
                grimPlayer.possibleInput = possibleInput;
                grimPlayer.predictedVelocity = possibleInputVelocityResult;

                Bukkit.broadcastMessage("Theoretical input " + grimPlayer.theoreticalInput + " size " + grimPlayer.theoreticalInput.lengthSquared());
            }
        }

        return grimPlayer.predictedVelocity.clone();
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
        double bestPossibleX;
        double bestPossibleZ;

        if (isSneaking) {
            bestPossibleX = Math.min(Math.max(-0.294, theoreticalInput.getX()), 0.294);
            bestPossibleZ = Math.min(Math.max(-0.294, theoreticalInput.getZ()), 0.294);
        } else {
            bestPossibleX = Math.min(Math.max(-0.98, theoreticalInput.getX()), 0.98);
            bestPossibleZ = Math.min(Math.max(-0.98, theoreticalInput.getZ()), 0.98);
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);

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

    public void addJumpIfNeeded(GrimPlayer grimPlayer) {
        // TODO: Make sure the player is actually on the ground
        // TODO: Add check to stop players from jumping more than once every 10 ticks
        double d7 = grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) > 0 ? grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) : grimPlayer.fluidHeight.getOrDefault(FluidTag.WATER, 0);
        boolean bl = grimPlayer.fluidHeight.getOrDefault(FluidTag.WATER, 0) > 0 && d7 > 0.0;
        double d8 = 0.4D;
        if (bl && (!grimPlayer.lastOnGround || d7 > d8)) {
            grimPlayer.clientVelocityJumping = grimPlayer.clientVelocity.clone().add(new Vector(0, 0.4, 0));
        } else if (grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) > 0 && (!grimPlayer.lastOnGround || d7 > d8)) {
            grimPlayer.clientVelocityJumping = grimPlayer.clientVelocity.clone().add(new Vector(0, 0.4, 0));
        } else if ((grimPlayer.lastOnGround || bl && d7 <= d8) /*&& this.noJumpDelay == 0*/) {
            grimPlayer.clientVelocityJumping = JumpPower.jumpFromGround(grimPlayer);
            //this.noJumpDelay = 10;
        }
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        return vector;
    }
}
