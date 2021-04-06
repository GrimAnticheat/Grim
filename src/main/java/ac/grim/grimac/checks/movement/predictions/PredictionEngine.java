package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementVelocityCheck;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.math.VectorPair;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
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

    public void guessBestMovement(float f, GrimPlayer grimPlayer) {
        List<VectorPair> possibleCombinations = new ArrayList<>();
        double bestInput = Double.MAX_VALUE;
        addJumpIfNeeded(grimPlayer);

        for (Vector possibleLastTickOutput : fetchPossibleInputs(grimPlayer)) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    possibleCombinations.add(new VectorPair(possibleLastTickOutput, getBestPossiblePlayerInput(grimPlayer, new Vector(x, 0, z))));
                }
            }
        }

        // This is an optimization - sort the inputs by the most likely first to stop running unneeded collisions
        possibleCombinations.sort((a, b) -> {
            if (a.lastTickOutput.clone().add(getMovementResultFromInput(a.playerInput, f, grimPlayer.xRot)).distanceSquared(grimPlayer.actualMovement) >
                    b.lastTickOutput.clone().add(getMovementResultFromInput(b.playerInput, f, grimPlayer.xRot)).distanceSquared(grimPlayer.actualMovement)) {
                return 1;
            } else {
                return -1;
            }
        });

        for (VectorPair possibleCollisionInputs : possibleCombinations) {
            Vector possibleInputVelocityResult = Collisions.collide(Collisions.maybeBackOffFromEdge(possibleCollisionInputs.lastTickOutput.clone().add(getMovementResultFromInput(possibleCollisionInputs.playerInput, f, grimPlayer.xRot)).multiply(grimPlayer.stuckSpeedMultiplier), MoverType.SELF, grimPlayer), grimPlayer);
            double resultAccuracy = possibleInputVelocityResult.distance(grimPlayer.actualMovement);

            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.possibleInput = possibleCollisionInputs.playerInput;
                grimPlayer.predictedVelocity = possibleInputVelocityResult;

                // Theoretical input exists for debugging purposes, no current use yet in checks.
                grimPlayer.theoreticalInput = getBestTheoreticalPlayerInput(grimPlayer.actualMovement.clone().subtract(possibleCollisionInputs.lastTickOutput).divide(grimPlayer.stuckSpeedMultiplier), f, grimPlayer.xRot);

                // Close enough.
                if (resultAccuracy < 0.001) break;
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
