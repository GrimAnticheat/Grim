package ac.grim.grimac.checks.movement.predictions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementVelocityCheck;
import ac.grim.grimac.utils.chunks.CachedContainsLiquid;
import ac.grim.grimac.utils.collisions.Collisions;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import net.minecraft.server.v1_16_R3.TagsFluid;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PredictionEngine {

    public static Vector getBestPossiblePlayerInput(GrimPlayer grimPlayer, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        if (grimPlayer.wasSneaking && !grimPlayer.isSwimming && !grimPlayer.specialFlying) {
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
        grimPlayer.speed = f;

        List<Vector> possibleVelocities = new ArrayList<>();
        double bestInput = Double.MAX_VALUE;

        // Stop omni-sprint
        // Optimization - Also cuts down needed possible inputs by 2/3
        int zMin = grimPlayer.isSprinting ? 1 : -1;
        for (Vector possibleLastTickOutput : fetchPossibleInputs(grimPlayer)) {
            for (int x = -1; x <= 1; x++) {
                for (int z = zMin; z <= 1; z++) {
                    possibleVelocities.add(handleOnClimbable(possibleLastTickOutput.clone().add(getMovementResultFromInput(getBestPossiblePlayerInput(grimPlayer, new Vector(x, 0, z)), f, grimPlayer.xRot)).multiply(grimPlayer.stuckSpeedMultiplier), grimPlayer));
                }
            }
        }

        // This is an optimization - sort the inputs by the most likely first to stop running unneeded collisions
        possibleVelocities.sort((a, b) -> compareDistanceToActualMovement(a, b, grimPlayer));


        // Other checks will catch ground spoofing - determine if the player can make an input below 0.03
        // Currently not used, but should be very helpful for handling movement below 0.03
        grimPlayer.couldSkipTick = false;
        if (grimPlayer.onGround) {
            possibleVelocities.forEach((a) -> grimPlayer.couldSkipTick = grimPlayer.couldSkipTick || a.getX() * a.getX() + a.getZ() * a.getZ() < 9.0E-4D);
        } else {
            possibleVelocities.forEach((a) -> grimPlayer.couldSkipTick = grimPlayer.couldSkipTick || a.getX() * a.getX() + a.getY() * a.getY() + a.getZ() + a.getZ() < 9.0E-4D);
        }

        Vector bestCollisionVel = null;


        for (Vector clientVelAfterInput : possibleVelocities) {
            // TODO: Player inputs should most likely be done before maybeBackOffOfEdge
            Vector backOff = Collisions.maybeBackOffFromEdge(clientVelAfterInput, MoverType.SELF, grimPlayer);
            Vector outputVel = Collisions.collide(grimPlayer, backOff.getX(), backOff.getY(), backOff.getZ());
            double resultAccuracy = outputVel.distance(grimPlayer.actualMovement);

            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                grimPlayer.clientVelocity = backOff.clone();
                bestCollisionVel = outputVel.clone();

                // Optimization - Close enough, other inputs won't get closer
                if (resultAccuracy < 0.01) break;
            }
        }

        grimPlayer.clientVelocity = MovementVelocityCheck.move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity, bestCollisionVel);
        grimPlayer.predictedVelocity = bestCollisionVel.clone();
        endOfTick(grimPlayer, grimPlayer.gravity, grimPlayer.friction);
    }

    public int compareDistanceToActualMovement(Vector a, Vector b, GrimPlayer grimPlayer) {
        double x = grimPlayer.actualMovement.getX();
        double y = grimPlayer.actualMovement.getY();
        double z = grimPlayer.actualMovement.getZ();

        // Weight y distance heavily to avoid jumping when we shouldn't be jumping, as it affects later ticks.
        double distance1 = Math.pow(a.getX() - x, 2) + Math.pow(a.getY() - y, 2) * 5 + Math.pow(a.getZ() - z, 2);
        double distance2 = Math.pow(b.getX() - x, 2) + Math.pow(b.getY() - y, 2) * 5 + Math.pow(b.getZ() - z, 2);

        if (distance1 > distance2) {
            return 1;
        } else if (distance1 == distance2) {
            return 0;
        }
        return -1;
    }

    public void addJumpsToPossibilities(GrimPlayer grimPlayer, Set<Vector> existingVelocities) {
        // TODO: Make sure the player is actually on the ground
        // TODO: Add check to stop players from jumping more than once every 10 ticks

        //for (Vector vector : existingVelocities) {
        //    existingVelocities.add(handleSwimJump(grimPlayer, vector));
        //}

        // Clone to stop ConcurrentModificationException
        for (Vector vector : new HashSet<>(existingVelocities)) {
            Vector clonedVector = vector.clone();
            doJump(grimPlayer, clonedVector);
            existingVelocities.add(clonedVector);
        }
    }

    public void doJump(GrimPlayer grimPlayer, Vector vector) {
        double d7 = grimPlayer.fluidHeight.getOrDefault(TagsFluid.LAVA, 0) > 0 ? grimPlayer.fluidHeight.getOrDefault(TagsFluid.LAVA, 0) : grimPlayer.fluidHeight.getOrDefault(TagsFluid.WATER, 0);
        boolean bl = grimPlayer.fluidHeight.getOrDefault(TagsFluid.WATER, 0) > 0 && d7 > 0.0;
        double d8 = 0.4D;

        if (!grimPlayer.specialFlying) {
            if (bl && (!grimPlayer.lastOnGround || d7 > d8)) {
                vector.add(new Vector(0, 0.4, 0));
            } else if (grimPlayer.fluidHeight.getOrDefault(TagsFluid.LAVA, 0) > 0 && (!grimPlayer.lastOnGround || d7 > d8)) {
                vector.add(new Vector(0, 0.4, 0));
            } else if ((grimPlayer.lastOnGround || bl && d7 <= d8) /*&& this.noJumpDelay == 0*/) {
                JumpPower.jumpFromGround(grimPlayer, vector);
            }
        } else {
            vector.add(new Vector(0, grimPlayer.flySpeed * 3, 0));
        }
    }

    public Set<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        Set<Vector> velocities = grimPlayer.getPossibleVelocities();

        addJumpsToPossibilities(grimPlayer, velocities);

        return velocities;
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        return vector;
    }

    public void endOfTick(GrimPlayer grimPlayer, double d, float friction) {
        grimPlayer.clientVelocitySwimHop = null;
        if (canSwimHop(grimPlayer, grimPlayer.clientVelocity)) {
            grimPlayer.clientVelocitySwimHop = grimPlayer.clientVelocity.clone().setY(0.3);
        }
    }

    public boolean canSwimHop(GrimPlayer grimPlayer, Vector vector) {
        boolean bl = Collisions.isEmpty(grimPlayer, grimPlayer.boundingBox.copy().expand(-0.1).offset(vector.getX(), 0.6, vector.getZ()));
        boolean bl2 = !Collisions.isEmpty(grimPlayer, grimPlayer.boundingBox.copy().expand(0.1, 0.1, 0.1));
        boolean bl3 = CachedContainsLiquid.containsLiquid(grimPlayer.boundingBox.copy().expand(0.1, 0.1, 0.1));

        // Vanilla system ->
        // Requirement 1 - The player must be in water or lava
        // Requirement 2 - The player must have X movement, Y movement + 0.6, Z movement no collision
        // Requirement 3 - The player must have horizontal collision

        // Our system ->
        // Requirement 1 - The player must be within 0.1 blocks of water or lava (which is why this is base and not PredictionEngineWater/Lava)
        // Requirement 2 - The player must have their bounding box plus X movement, Y movement + 0.6, Z movement minus 0.1 blocks have no collision
        // Requirement 3 - The player must have something to collide with within 0.1 blocks

        return bl && bl2 && bl3;
    }
}
