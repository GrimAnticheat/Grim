package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.JumpPower;
import org.bukkit.util.Vector;

public abstract class PredictionEngine {
    // We use the fact that the client already does collision to do predictions fast
    // Combined with our controller support for eventual geyser support
    // We can use non-whole inputs, such as (0.9217, 0.1599)
    // On legit players, running collision after guessing movement will never be an issue
    // On players with noclip and other cheats, it will flag the anticheat
    // We now only run 1 collision
    public Vector guessBestMovement(float f, GrimPlayer grimPlayer) {
        double bestInput = Double.MAX_VALUE;
        addJumpIfNeeded(grimPlayer);

        for (Vector possibleLastTickOutput : grimPlayer.getPossibleVelocities()) {
            // This method clamps climbing velocity (as in vanilla), if needed.
            possibleLastTickOutput = handleOnClimbable(possibleLastTickOutput, grimPlayer);

            Vector theoreticalInput = getBestTheoreticalPlayerInput(grimPlayer.actualMovement.clone().subtract(possibleLastTickOutput), f, grimPlayer.xRot);
            Vector possibleInput = getBestPossiblePlayerInput(grimPlayer, theoreticalInput);
            Vector possibleInputVelocityResult = possibleLastTickOutput.clone().add(getMovementResultFromInput(possibleInput, f, grimPlayer.xRot));

            double resultAccuracy = possibleInputVelocityResult.distanceSquared(grimPlayer.actualMovement);

            //Bukkit.broadcastMessage("Accuracy for " + possibleInputVelocityResult + " " + resultAccuracy);
            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.bestOutput = possibleLastTickOutput;
                grimPlayer.theoreticalInput = theoreticalInput;
                grimPlayer.possibleInput = possibleInput;
                grimPlayer.predictedVelocity = possibleInputVelocityResult;

                //Bukkit.broadcastMessage("Theoretical input " + grimPlayer.theoreticalInput + " size " + grimPlayer.theoreticalInput.lengthSquared());
            }
        }

        return grimPlayer.predictedVelocity;
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

    /*public static Vector getBestPossiblePlayerInput(boolean isSneaking, Vector theoreticalInput) {
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
    }*/

    // These math equations are based off of the vanilla equations, made impossible to divide by 0
    public static Vector getBestTheoreticalPlayerInput(Vector wantedMovement, float f, float f2) {
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public static Vector getBestPossiblePlayerInput(GrimPlayer grimPlayer, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        if (grimPlayer.isSneaking && !grimPlayer.bukkitPlayer.isSwimming()) {
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
    // We need it because of collision support when a player is using speed
    public static Vector getMovementResultFromInput(Vector inputVector, float f, float f2) {
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);

        double xResult = inputVector.getX() * f4 - inputVector.getZ() * f3;
        double zResult = inputVector.getZ() * f4 + inputVector.getX() * f3;

        return new Vector(xResult * f, 0, zResult * f);
    }
}
