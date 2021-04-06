package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementVelocityCheck;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public abstract class PredictionEngine {
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

        if (grimPlayer.isSneaking && !grimPlayer.bukkitPlayer.isSwimming() && !grimPlayer.bukkitPlayer.isFlying()) {
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

    // We use the fact that the client already does collision to do predictions fast
    // Combined with our controller support for eventual geyser support
    // We can use non-whole inputs, such as (0.9217, 0.1599)
    // On legit players, running collision after guessing movement will never be an issue
    // On players with noclip and other cheats, it will flag the anticheat
    // We now only run 1 collision
    public void guessBestMovement(float f, GrimPlayer grimPlayer) {
        double bestInput = Double.MAX_VALUE;
        addJumpIfNeeded(grimPlayer);

        // TODO: Readd support for jumping
        for (Vector possibleLastTickOutput : fetchPossibleInputs(grimPlayer)) {
            //Bukkit.broadcastMessage("Possible out " + possibleLastTickOutput);

            // This method clamps climbing velocity (as in vanilla), if needed.
            possibleLastTickOutput = handleOnClimbable(possibleLastTickOutput, grimPlayer);
            Vector theoreticalInput = getBestTheoreticalPlayerInput(grimPlayer.actualMovement.clone().subtract(possibleLastTickOutput).divide(grimPlayer.stuckSpeedMultiplier), f, grimPlayer.xRot);
            Vector possibleInput = getBestPossiblePlayerInput(grimPlayer, theoreticalInput);
            Vector possibleInputVelocityResult = possibleLastTickOutput.clone().add(getMovementResultFromInput(possibleInput, f, grimPlayer.xRot));

            double resultAccuracy = possibleInputVelocityResult.setY(0).distance(grimPlayer.actualMovement.clone().setY(0));

            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.bestOutput = possibleLastTickOutput;
                grimPlayer.theoreticalInput = theoreticalInput;
                grimPlayer.possibleInput = possibleInput;
                grimPlayer.predictedVelocity = possibleInputVelocityResult.multiply(grimPlayer.stuckSpeedMultiplier);

                Bukkit.broadcastMessage("Useful input " + grimPlayer.possibleInput + " accuracy " + resultAccuracy + " result " + possibleInputVelocityResult + " wanted " + grimPlayer.actualMovement);
            }
        }

        // The player probably collided against something, sacrifice some optimization for accuracy
        // TODO: Readd support for jumping
        if (bestInput > 0.01) {
            for (Vector possibleLastTickOutput : fetchPossibleInputs(grimPlayer)) {
                // We run this calculation twice, perhaps don't do this?
                Vector wantedMovement = grimPlayer.actualMovement.clone().setY(0);
                List<Vector> possibleInputs = new ArrayList<>();

                for (int x = 0; x <= 0; x++) {
                    for (int z = 1; z <= 1; z++) {
                        // If the input is going in the wrong direction, we shouldn't try it.
                        // Forwards input can go right with collisions
                        // Look at the directions marked with a ! to see the possible ones
                        //
                        // The player is looking slightly right than directly straight
                        // =================================================
                        //                    \ ^(!) / (!)
                        //                 <--- *(!) ---> (!)   Wanted Direction ------>
                        //                     / \/ \ (!)
                        //
                        // As you see we are able to eliminate 4 inputs and collisions by this line!
                        // It is 195 instead of 180 to try and reduce eliminating inputs that could be possible
                        // Shouldn't really matter but let's be on the safe side of optimization.
                        Vector input = new Vector(x, 0, z);

                        if (input.angle(wantedMovement) > 195) continue;
                        possibleInputs.add(input);
                    }
                }

                // This should NOT be possible but a REALLY bad prediction before this could make it possible
                if (grimPlayer.possibleInput.getX() != 0 || grimPlayer.possibleInput.getZ() != 0) {
                    possibleInputs.add(new Vector(0, 0, 0));
                }

                for (Vector possibleCollisionInputs : possibleInputs) {
                    Vector possibleInput = getBestPossiblePlayerInput(grimPlayer, possibleCollisionInputs);

                    Vector possibleInputVelocityResult = Collisions.collide(Collisions.maybeBackOffFromEdge(possibleLastTickOutput.clone().add(getMovementResultFromInput(possibleInput, f, grimPlayer.xRot)).multiply(grimPlayer.stuckSpeedMultiplier), MoverType.SELF, grimPlayer), grimPlayer);
                    double resultAccuracy = possibleInputVelocityResult.setY(0).distance(wantedMovement);

                    Bukkit.broadcastMessage("Last closeness " + bestInput + "Possible input " + possibleInput + " Prior" + possibleLastTickOutput + " Input result " + possibleInputVelocityResult + "Possible input " + possibleInput + " accuracy " + resultAccuracy);

                    // Don't touch theoretical input, that was calculated earlier and is correct
                    if (resultAccuracy < bestInput) {
                        Bukkit.broadcastMessage(ChatColor.RED + "Using collision");
                        bestInput = resultAccuracy;
                        grimPlayer.bestOutput = possibleLastTickOutput;
                        grimPlayer.possibleInput = possibleInput;
                        grimPlayer.predictedVelocity = possibleInputVelocityResult;
                    }
                }
            }
        }

        grimPlayer.clientVelocity = MovementVelocityCheck.move(grimPlayer, MoverType.SELF, grimPlayer.predictedVelocity);
        grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
        endOfTick(grimPlayer, grimPlayer.gravity, grimPlayer.friction);
    }

    public void addJumpIfNeeded(GrimPlayer grimPlayer) {
        // TODO: Make sure the player is actually on the ground
        // TODO: Add check to stop players from jumping more than once every 10 ticks

        handleSwimJump(grimPlayer, grimPlayer.clientVelocity);

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

    public List<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        return grimPlayer.getPossibleVelocities();
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        return vector;
    }

    public void endOfTick(GrimPlayer grimPlayer, double d, float friction) {

    }

    private void handleSwimJump(GrimPlayer grimPlayer, Vector vector) {
        if (grimPlayer.possibleKnockback.contains(vector)) return;

        AxisAlignedBB isByLiquid = grimPlayer.entityPlayer.getBoundingBox().grow(0.1, 0, 0.1);

        /*boolean bl = grimPlayer.entityPlayer.world.getCubes(grimPlayer.entityPlayer, grimPlayer.entityPlayer.getBoundingBox().shrink(0.1).d(vector.getX(), 0.6, vector.getZ()));
        boolean bl2 = !grimPlayer.entityPlayer.world.getCubes(grimPlayer.entityPlayer, isByLiquid);
        boolean bl3 = grimPlayer.entityPlayer.world.containsLiquid(isByLiquid);

        // Vanilla system ->
        // Requirement 1 - The player must be in water or lava
        // Requirement 2 - The player must have X movement, Y movement + 0.6, Z movement no collision
        // Requirement 3 - The player must have horizontal collision

        // Our system ->
        // Requirement 1 - The player must be within 0.1 blocks of water or lava (which is why this is base and not PredictionEngineWater/Lava)
        // Requirement 2 - The player must have their bounding box plus X movement, Y movement + 0.6, Z movement minus 0.1 blocks have no collision
        // Requirement 3 - The player must have something to collide with within 0.1 blocks

        if (bl && bl2 && bl3) {
            grimPlayer.clientVelocitySwimHop = grimPlayer.clientVelocity.clone().setY(0.3);
        }*/
    }
}
