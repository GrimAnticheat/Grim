package ac.grim.grimac.checks.predictionengine.predictions;

import ac.grim.grimac.checks.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.CachedContainsLiquid;
import ac.grim.grimac.utils.collisions.Collisions;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PredictionEngine {

    public static Vector getBestPossiblePlayerInput(GrimPlayer player, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        // We save the slow movement status as it's easier and takes less CPU than recalculating it with newly stored old values
        if (player.isSlowMovement) {
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

    public void guessBestMovement(float speed, GrimPlayer player) {
        player.speed = speed;
        double bestInput = Double.MAX_VALUE;

        List<VectorData> possibleVelocities = multiplyPossibilitiesByInputs(player, fetchPossibleInputs(player), speed);

        // This is an optimization - sort the inputs by the most likely first to stop running unneeded collisions
        possibleVelocities.sort((a, b) -> compareDistanceToActualMovement(a.vector, b.vector, player));

        // Other checks will catch ground spoofing - determine if the player can make an input below 0.03
        player.couldSkipTick = false;
        if (player.onGround) {
            possibleVelocities.forEach((a) -> player.couldSkipTick = player.couldSkipTick || a.vector.getX() * a.vector.getX() + a.vector.getZ() * a.vector.getZ() < 9.0E-4D);
        } else {
            possibleVelocities.forEach((a) -> player.couldSkipTick = player.couldSkipTick || a.vector.getX() * a.vector.getX() + a.vector.getY() * a.vector.getY() + a.vector.getZ() + a.vector.getZ() < 9.0E-4D);
        }

        VectorData bestCollisionVel = null;

        for (VectorData clientVelAfterInput : possibleVelocities) {
            // TODO: Player inputs should most likely be done before maybeBackOffOfEdge
            Vector backOff = Collisions.maybeBackOffFromEdge(clientVelAfterInput.vector, MoverType.SELF, player);
            Vector outputVel = Collisions.collide(player, backOff.getX(), backOff.getY(), backOff.getZ());
            double resultAccuracy = outputVel.distance(player.actualMovement);

            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;
                player.clientVelocity = backOff.clone();
                bestCollisionVel = new VectorData(outputVel.clone(), clientVelAfterInput.vectorType);

                // Optimization - Close enough, other inputs won't get closer
                if (resultAccuracy < 0.01) break;
            }
        }

        new MovementTickerPlayer(player).move(MoverType.SELF, player.clientVelocity, bestCollisionVel.vector);
        player.predictedVelocity = bestCollisionVel.vector.clone();
        endOfTick(player, player.gravity, player.friction);
    }

    public int compareDistanceToActualMovement(Vector a, Vector b, GrimPlayer player) {
        double x = player.actualMovement.getX();
        double y = player.actualMovement.getY();
        double z = player.actualMovement.getZ();

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

    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        // TODO: Make sure the player is actually on the ground
        // TODO: Add check to stop players from jumping more than once every 10 ticks

        for (VectorData vector : new HashSet<>(existingVelocities)) {
            Vector clonedVector = vector.vector.clone();
            doJump(player, vector.vector);
            existingVelocities.add(new VectorData(clonedVector, vector.vectorType));
        }
    }

    public void addAdditionToPossibleVectors(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            // TODO: Add only the stuff the player has received
            for (Vector explosion : player.compensatedExplosion.getPossibleExplosions(player.lastTransactionReceived)) {
                Vector clonedVector = vector.vector.clone();
                clonedVector.add(explosion);
                existingVelocities.add(new VectorData(clonedVector, vector.vectorType));
            }
        }
    }

    public void doJump(GrimPlayer player, Vector vector) {
        double d7 = player.fluidHeight.getOrDefault(FluidTag.LAVA, 0) > 0 ? player.fluidHeight.getOrDefault(FluidTag.LAVA, 0) : player.fluidHeight.getOrDefault(FluidTag.WATER, 0);
        boolean bl = player.fluidHeight.getOrDefault(FluidTag.WATER, 0) > 0 && d7 > 0.0;
        double d8 = 0.4D;

        if (!player.specialFlying) {
            if (bl && (!player.lastOnGround || d7 > d8)) {
                vector.add(new Vector(0, 0.4, 0));
            } else if (player.fluidHeight.getOrDefault(FluidTag.LAVA, 0) > 0 && (!player.lastOnGround || d7 > d8)) {
                vector.add(new Vector(0, 0.4, 0));
            } else if ((player.lastOnGround || bl && d7 <= d8) /*&& this.noJumpDelay == 0*/) {
                JumpPower.jumpFromGround(player, vector);
            }
        } else {
            vector.add(new Vector(0, player.flySpeed * 3, 0));
        }
    }

    public List<VectorData> multiplyPossibilitiesByInputs(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        // Stop omni-sprint
        // Optimization - Also cuts down needed possible inputs by 2/3
        int zMin = player.isSprinting ? 1 : -1;
        List<VectorData> returnVectors = new ArrayList<>();

        for (VectorData possibleLastTickOutput : possibleVectors) {
            for (int x = -1; x <= 1; x++) {
                for (int z = zMin; z <= 1; z++) {
                    returnVectors.add(new VectorData(handleOnClimbable(possibleLastTickOutput.vector.clone().add(getMovementResultFromInput(getBestPossiblePlayerInput(player, new Vector(x, 0, z)), speed, player.xRot)).multiply(player.stuckSpeedMultiplier), player), possibleLastTickOutput.vectorType));
                }
            }
        }

        return returnVectors;
    }

    public Set<VectorData> fetchPossibleInputs(GrimPlayer player) {
        Set<VectorData> velocities = player.getPossibleVelocities();

        addAdditionToPossibleVectors(player, velocities);
        addJumpsToPossibilities(player, velocities);

        return velocities;
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        return vector;
    }

    public void endOfTick(GrimPlayer player, double d, float friction) {
        player.clientVelocitySwimHop = null;
        if (canSwimHop(player, player.clientVelocity)) {
            player.clientVelocitySwimHop = player.clientVelocity.clone().setY(0.3);
        }
    }

    public boolean canSwimHop(GrimPlayer player, Vector vector) {
        boolean canCollideHorizontally = !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.1, -0.01, 0.1));

        SimpleCollisionBox isFreeBox = GetBoundingBox.getPlayerBoundingBox(player, player.x, player.y, player.z).offset(vector.getX(), vector.getY() + 0.6 - player.y + player.lastY, vector.getZ());

        boolean isFree = Collisions.isEmpty(player, isFreeBox);
        // TODO: Can we just use .wasTouchingWater or does the < 0.03 mess it up too much.
        boolean inWater = CachedContainsLiquid.containsLiquid(player, player.boundingBox.copy().expand(0.1, 0.1, 0.1));

        // Vanilla system ->
        // Requirement 1 - The player must be in water or lava
        // Requirement 2 - The player must have X position + X movement, Y position + Y movement - Y position before tick + 0.6, Z position + Z movement have no collision
        // Requirement 3 - The player must have horizontal collision

        // Our system ->
        // Requirement 1 - The player must be within 0.1 blocks of water or lava (which is why this is base and not PredictionEngineWater/Lava)
        // Requirement 2 - The player must have their bounding box plus X movement, Y movement + 0.6, Z movement minus 0.1 blocks have no collision
        // Requirement 3 - The player must have something to collide with within 0.1 blocks

        return canCollideHorizontally && isFree && inWater;
    }
}
