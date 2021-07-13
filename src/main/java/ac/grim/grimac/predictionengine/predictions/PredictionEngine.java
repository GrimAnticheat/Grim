package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class PredictionEngine {
    boolean canRiptide = false;

    public void guessBestMovement(float speed, GrimPlayer player) {
        player.speed = speed;
        double bestInput = Double.MAX_VALUE;

        List<VectorData> possibleVelocities = applyInputsToVelocityPossibilities(player, fetchPossibleStartTickVectors(player), speed);

        // Other checks will catch ground spoofing - determine if the player can make an input below 0.03
        player.couldSkipTick = false;
        if (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree) {
            possibleVelocities.forEach((a) -> player.couldSkipTick = player.couldSkipTick || a.vector.getX() * a.vector.getX() + a.vector.getZ() * a.vector.getZ() < 0.0016);
        } else {
            possibleVelocities.forEach((a) -> player.couldSkipTick = player.couldSkipTick || a.vector.lengthSquared() < 0.0016);
        }

        if (player.couldSkipTick) {
            Set<VectorData> zeroStuff = new HashSet<>();
            zeroStuff.add(new VectorData(new Vector().setY(player.clientVelocity.getY()), VectorData.VectorType.ZeroPointZeroThree));
            addJumpsToPossibilities(player, zeroStuff);
            possibleVelocities.addAll(applyInputsToVelocityPossibilities(player, zeroStuff, speed));

            double yVelocity = player.clientVelocity.getY();

            if (Math.abs(yVelocity) < 0.03) {
                yVelocity -= 0.08;

                player.uncertaintyHandler.gravityUncertainty = yVelocity;
            }
        }

        // Sorting is an optimization and a requirement
        possibleVelocities.sort((a, b) -> sortVectorData(a, b, player));

        VectorData bestCollisionVel = null;
        Vector beforeCollisionMovement = null;
        Vector tempClientVelChosen = null;

        for (VectorData clientVelAfterInput : possibleVelocities) {
            Vector primaryPushMovement = handleStartingVelocityUncertainty(player, clientVelAfterInput);
            Vector backOff = Collisions.maybeBackOffFromEdge(primaryPushMovement, player);
            Vector additionalPushMovement = handlePushMovementThatDoesntAffectNextTickVel(player, backOff);
            Vector outputVel = Collisions.collide(player, additionalPushMovement.getX(), additionalPushMovement.getY(), additionalPushMovement.getZ());
            double resultAccuracy = outputVel.distanceSquared(player.actualMovement);

            if (resultAccuracy < bestInput) {
                bestInput = resultAccuracy;

                bestCollisionVel = clientVelAfterInput.setVector(outputVel, VectorData.VectorType.BestVelPicked);
                beforeCollisionMovement = additionalPushMovement;
                tempClientVelChosen = primaryPushMovement.clone();

                // Optimization - Close enough, other inputs won't get closer
                // This works as knockback and explosions are ran first
                //
                // Note that sometimes the first and closest velocity isn't the closest because collisions
                // The player may only be able to move a slight amount compared to what the initial vector shows
                if (resultAccuracy < 1e-6) break;
            }
        }

        // The player always has at least one velocity - clientVelocity
        assert bestCollisionVel != null;
        player.clientVelocity = tempClientVelChosen;
        new MovementTickerPlayer(player).move(beforeCollisionMovement, bestCollisionVel.vector);
        player.predictedVelocity = bestCollisionVel;
        endOfTick(player, player.gravity, player.friction);
    }

    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> returnVectors = new ArrayList<>();
        loopVectors(player, possibleVectors, speed, returnVectors);

        // There is a bug where the player sends sprinting, thinks they are sprinting, server also thinks so, but they don't have sprinting speed
        // It mostly occurs when the player takes damage.
        // This isn't going to destroy predictions as sprinting uses 1/3 the number of inputs, now 2/3 with this hack
        // Meaning there is still a 1/3 improvement for sprinting players over non-sprinting
        // If a player in this glitched state lets go of moving forward, then become un-glitched
        if (player.isSprinting) {
            player.isSprinting = false;
            speed /= 1.3D;
            loopVectors(player, possibleVectors, speed, returnVectors);
            player.isSprinting = true;
        }

        return returnVectors;
    }

    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        Set<VectorData> velocities = player.getPossibleVelocities();

        addExplosionRiptideToPossibilities(player, velocities);
        addJumpsToPossibilities(player, velocities);

        return velocities;
    }

    public int sortVectorData(VectorData a, VectorData b, GrimPlayer player) {
        int aScore = 0;
        int bScore = 0;

        // Fixes false using riptide under 2 blocks of water
        boolean aTridentJump = a.hasVectorType(VectorData.VectorType.Trident) && !a.hasVectorType(VectorData.VectorType.Jump);
        boolean bTridentJump = b.hasVectorType(VectorData.VectorType.Trident) && !b.hasVectorType(VectorData.VectorType.Jump);

        if (aTridentJump && !bTridentJump)
            return -1;

        if (bTridentJump && !aTridentJump)
            return 1;

        // Put explosions and knockback first so they are applied to the player
        // Otherwise the anticheat can't handle minor knockback and explosions without knowing if the player took the kb
        if (a.hasVectorType(VectorData.VectorType.Explosion))
            aScore++;

        if (a.hasVectorType(VectorData.VectorType.Knockback))
            aScore++;

        if (b.hasVectorType(VectorData.VectorType.Explosion))
            bScore++;

        if (b.hasVectorType(VectorData.VectorType.Knockback))
            bScore++;

        if (aScore != bScore)
            return Integer.compare(aScore, bScore);

        // If all else fails, just compare the distance and use the one closest to the player
        // It's an optimization and isn't really required
        double x = player.actualMovement.getX();
        double y = player.actualMovement.getY();
        double z = player.actualMovement.getZ();

        // Weight y distance heavily to avoid jumping when we shouldn't be jumping, as it affects later ticks.
        // Issue with this mainly occurs with < 0.03 movement in stuff such as cobwebs
        double distance1 = Math.pow(a.vector.getX() - x, 2) + Math.pow(a.vector.getY() - y, 2) * 5 + Math.pow(a.vector.getZ() - z, 2);
        double distance2 = Math.pow(b.vector.getX() - x, 2) + Math.pow(b.vector.getY() - y, 2) * 5 + Math.pow(b.vector.getZ() - z, 2);

        return Double.compare(distance1, distance2);
    }

    private Vector handleStartingVelocityUncertainty(GrimPlayer player, VectorData vector) {
        // Give 0.06 lenience when zero tick
        return getStartingVector(player, vector.vector, vector.hasVectorType(VectorData.VectorType.ZeroPointZeroThree) ? 0.06 : player.uncertaintyHandler.lastMovementWasZeroPointZeroThree ? 0.06 : player.uncertaintyHandler.lastLastMovementWasZeroPointZeroThree ? 0.03 : 0);
    }

    public Vector handlePushMovementThatDoesntAffectNextTickVel(GrimPlayer player, Vector vector) {
        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        double avgColliding = GrimMathHelper.calculateAverage(player.uncertaintyHandler.collidingEntities);

        // 0.03 was falsing when colliding with https://i.imgur.com/7obfxG6.png
        // 0.04 is safe from falses
        // Set to 0.06 because this is a very stupid reason to allow falses
        //
        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        Vector uncertainty = new Vector(player.uncertaintyHandler.pistonX + avgColliding * 0.065, player.uncertaintyHandler.pistonY, player.uncertaintyHandler.pistonZ + avgColliding * 0.065);
        return PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement,
                vector.clone().add(uncertainty.clone().multiply(-1)).add(new Vector(0, player.uncertaintyHandler.wasLastOnGroundUncertain ? -0.03 : 0, 0)),
                vector.clone().add(uncertainty).add(new Vector(0, player.canGroundRiptide ? 1.1999999F : 0, 0)));
    }

    public void endOfTick(GrimPlayer player, double d, float friction) {
        player.clientVelocitySwimHop = null;
        if (canSwimHop(player)) {
            player.clientVelocitySwimHop = player.clientVelocity.clone().setY(0.3);
        }
    }

    private void loopVectors(GrimPlayer player, Set<VectorData> possibleVectors, float speed, List<VectorData> returnVectors) {
        // Stop omni-sprint
        // Optimization - Also cuts down scenarios by 2/3
        // For some reason the player sprints while swimming no matter what
        // Probably as a way to tell the server it is swimming
        int zMin = player.isSprinting && !player.isSwimming ? 1 : -1;

        for (VectorData possibleLastTickOutput : possibleVectors) {
            for (int x = -1; x <= 1; x++) {
                for (int z = zMin; z <= 1; z++) {
                    VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(getMovementResultFromInput(player, transformInputsToVector(player, new Vector(x, 0, z)), speed, player.xRot)), possibleLastTickOutput, VectorData.VectorType.InputResult);
                    result = result.setVector(handleFireworkMovementLenience(player, result.vector.clone()), VectorData.VectorType.Lenience);
                    result = result.setVector(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
                    result = result.setVector(handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
                    returnVectors.add(result);
                }
            }
        }
    }

    public void addExplosionRiptideToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.knownExplosion != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.knownExplosion.vector), vector, VectorData.VectorType.Explosion));
            }

            if (player.firstBreadExplosion != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.firstBreadExplosion.vector), vector, VectorData.VectorType.Explosion));
            }
        }

        if (player.tryingToRiptide) {
            ItemStack main = player.bukkitPlayer.getInventory().getItemInMainHand();
            ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();

            int j;
            if (main.getType() == Material.TRIDENT) {
                j = main.getEnchantmentLevel(Enchantment.RIPTIDE);
            } else if (off.getType() == Material.TRIDENT) {
                j = off.getEnchantmentLevel(Enchantment.RIPTIDE);
            } else {
                return;
            }

            canRiptide = true;

            float f7 = player.xRot;
            float f = player.yRot;
            float f1 = -player.trigHandler.sin(f7 * ((float) Math.PI / 180F)) * player.trigHandler.cos(f * ((float) Math.PI / 180F));
            float f2 = -player.trigHandler.sin(f * ((float) Math.PI / 180F));
            float f3 = player.trigHandler.cos(f7 * ((float) Math.PI / 180F)) * player.trigHandler.cos(f * ((float) Math.PI / 180F));
            float f4 = (float) Math.sqrt(f1 * f1 + f2 * f2 + f3 * f3);
            float f5 = 3.0F * ((1.0F + (float) j) / 4.0F);
            f1 = f1 * (f5 / f4);
            f2 = f2 * (f5 / f4);
            f3 = f3 * (f5 / f4);

            existingVelocities.add(new VectorData(player.clientVelocity.clone().add(new Vector(f1, f2, f3)), VectorData.VectorType.Trident));
        }
    }

    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
    }

    private Vector getStartingVector(GrimPlayer player, Vector vector, double addition) {
        double avgColliding = GrimMathHelper.calculateAverage(player.uncertaintyHandler.strictCollidingEntities);

        Vector uncertainty = new Vector(avgColliding * 0.04, 0, avgColliding * 0.04);
        Vector min = new Vector(player.uncertaintyHandler.xNegativeUncertainty - addition, player.uncertaintyHandler.gravityUncertainty - (player.uncertaintyHandler.wasLastGravityUncertain ? 0.03 : 0), player.uncertaintyHandler.zNegativeUncertainty - addition);
        Vector max = new Vector(player.uncertaintyHandler.xPositiveUncertainty + addition, player.uncertaintyHandler.lastLastPacketWasGroundPacket || player.uncertaintyHandler.isSteppingOnSlime ? 0.03 : 0, player.uncertaintyHandler.zPositiveUncertainty + addition);

        Vector minVector = vector.clone().add(min.subtract(uncertainty));
        Vector maxVector = vector.clone().add(max.add(uncertainty));

        // Player velocity can multiply 0.4-0.45 (guess on max) when the player is on slime with
        // a Y velocity of 0 to 0.1.  Because 0.03 we don't know this so just give lenience here
        if (player.uncertaintyHandler.isSteppingOnSlime) {
            if (vector.getX() > 0) {
                minVector.multiply(new Vector(0.4, 1, 1));
            } else {
                maxVector.multiply(new Vector(0.4, 1, 1));
            }

            if (vector.getZ() > 0) {
                minVector.multiply(new Vector(1, 1, 0.4));
            } else {
                maxVector.multiply(new Vector(1, 1, 0.4));
            }
        }

        if ((player.uncertaintyHandler.wasLastOnGroundUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket) && vector.getY() < 0) {
            maxVector.setY(0);
        }

        return PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement, minVector, maxVector);
    }

    public boolean canSwimHop(GrimPlayer player) {
        boolean canCollideHorizontally = !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -0.01, 0.5));
        boolean inWater = player.compensatedWorld.containsLiquid(player.boundingBox.copy().expand(0.1, 0.1, 0.1));

        // Vanilla system ->
        // Requirement 1 - The player must be in water or lava
        // Requirement 2 - The player must have X position + X movement, Y position + Y movement - Y position before tick + 0.6, Z position + Z movement have no collision
        // Requirement 3 - The player must have horizontal collision

        // Our system ->
        // Requirement 1 - The player must be within 0.1 blocks of water or lava (which is why this is base and not PredictionEngineWater/Lava)
        // Requirement 2 - The player must have something to collide with within 0.1 blocks

        // Why remove the empty check?  The real movement is hidden due to the horizontal collision
        // For example, a 1.14+ player can have a velocity of (10000, 0, 0) and if they are against a wall,
        // We only see the (0,0,0) velocity.
        // This means it is impossible to accurately create the requirement of no collision.
        // Oh well, I guess this could allow some Jesus bypasses next to a wall that has multiple blocks
        // But it's faster to swim anyways on 1.13+, and faster to just go on land in 1.12-

        // Oh, also don't forget that the player can swim hop when colliding with boats (and shulkers)
        // Just give a high lenience to this... not worth the risk of falses

        return canCollideHorizontally && inWater;
    }

    // This is just the vanilla equation, which accepts invalid inputs greater than 1
    // We need it because of collision support when a player is using speed
    public Vector getMovementResultFromInput(GrimPlayer player, Vector inputVector, float f, float f2) {
        float f3 = player.trigHandler.sin(f2 * 0.017453292f);
        float f4 = player.trigHandler.cos(f2 * 0.017453292f);

        double xResult = inputVector.getX() * f4 - inputVector.getZ() * f3;
        double zResult = inputVector.getZ() * f4 + inputVector.getX() * f3;

        return new Vector(xResult * f, 0, zResult * f);
    }

    public static Vector transformInputsToVector(GrimPlayer player, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        // Slow movement was determined by the previous pose
        if (player.isSlowMovement) {
            bestPossibleX = Math.min(Math.max(-1, Math.round(theoreticalInput.getX() / 0.3)), 1) * 0.3f;
            bestPossibleZ = Math.min(Math.max(-1, Math.round(theoreticalInput.getZ() / 0.3)), 1) * 0.3f;
        } else {
            bestPossibleX = Math.min(Math.max(-1, Math.round(theoreticalInput.getX())), 1);
            bestPossibleZ = Math.min(Math.max(-1, Math.round(theoreticalInput.getZ())), 1);
        }

        if (player.isUsingItem) {
            bestPossibleX *= 0.2F;
            bestPossibleZ *= 0.2F;
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);
        inputVector.multiply(0.98);

        // Simulate float rounding imprecision
        inputVector = new Vector((float) inputVector.getX(), (float) inputVector.getY(), (float) inputVector.getZ());

        if (inputVector.lengthSquared() > 1) {
            double d0 = ((float) Math.sqrt(inputVector.getX() * inputVector.getX() + inputVector.getY() * inputVector.getY() + inputVector.getZ() * inputVector.getZ()));
            inputVector = new Vector(inputVector.getX() / d0, inputVector.getY() / d0, inputVector.getZ() / d0);
        }

        return inputVector;
    }

    private Vector handleFireworkMovementLenience(GrimPlayer player, Vector vector) {
        int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;

        if (maxFireworks <= 0) return vector;
        if (!player.isGliding) return vector;

        Vector currentLook = PredictionEngineElytra.getVectorForRotation(player, player.yRot, player.xRot);
        Vector lastLook = PredictionEngineElytra.getVectorForRotation(player, player.lastYRot, player.lastXRot);

        Vector boostOne = vector.clone();
        Vector boostTwo = vector.clone();

        for (int i = 0; i < maxFireworks; i++) {
            boostOne.add(new Vector(currentLook.getX() * 0.1 + (currentLook.getX() * 1.5 - boostOne.getX()) * 0.5, currentLook.getY() * 0.1 + (currentLook.getY() * 1.5 - boostOne.getY()) * 0.5, (currentLook.getZ() * 0.1 + (currentLook.getZ() * 1.5 - boostOne.getZ()) * 0.5)));
            boostTwo.add(new Vector(lastLook.getX() * 0.1 + (lastLook.getX() * 1.5 - boostTwo.getX()) * 0.5, lastLook.getY() * 0.1 + (lastLook.getY() * 1.5 - boostTwo.getY()) * 0.5, (lastLook.getZ() * 0.1 + (lastLook.getZ() * 1.5 - boostTwo.getZ()) * 0.5)));
        }

        SimpleCollisionBox box = new SimpleCollisionBox(boostOne, boostTwo);

        if (box.minX > vector.getX()) {
            box.minX = vector.getX();
        } else if (box.maxX < vector.getX()) {
            box.maxX = vector.getX();
        }

        if (box.minY > vector.getY()) {
            box.minY = vector.getY();
        } else if (box.maxY < vector.getY()) {
            box.maxY = vector.getY();
        }

        if (box.minZ > vector.getZ()) {
            box.minZ = vector.getZ();
        } else if (box.maxZ < vector.getZ()) {
            box.maxZ = vector.getZ();
        }

        return PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement,
                new Vector(box.minX, box.minY, box.minZ),
                new Vector(box.maxX, box.maxY, box.maxZ));
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        return vector;
    }

    public void doJump(GrimPlayer player, Vector vector) {
        if (!player.lastOnGround)
            return;

        JumpPower.jumpFromGround(player, vector);
    }
}
