package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class PredictionEngine {
    boolean canRiptide = false;

    public static Vector transformInputsToVector(GrimPlayer player, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        // Slow movement was determined by the previous pose
        if (player.isSlowMovement) {
            bestPossibleX = (float) (Math.min(Math.max(-1f, Math.round(theoreticalInput.getX() / 0.3)), 1f) * 0.3d);
            bestPossibleZ = (float) (Math.min(Math.max(-1f, Math.round(theoreticalInput.getZ() / 0.3)), 1f) * 0.3d);
        } else {
            bestPossibleX = Math.min(Math.max(-1f, Math.round(theoreticalInput.getX())), 1f);
            bestPossibleZ = Math.min(Math.max(-1f, Math.round(theoreticalInput.getZ())), 1f);
        }

        if (player.isUsingItem == AlmostBoolean.TRUE || player.isUsingItem == AlmostBoolean.MAYBE) {
            bestPossibleX *= 0.2F;
            bestPossibleZ *= 0.2F;
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);
        inputVector.multiply(0.98F);

        // Simulate float rounding imprecision
        inputVector = new Vector((float) inputVector.getX(), (float) inputVector.getY(), (float) inputVector.getZ());

        if (inputVector.lengthSquared() > 1) {
            double d0 = ((float) Math.sqrt(inputVector.getX() * inputVector.getX() + inputVector.getY() * inputVector.getY() + inputVector.getZ() * inputVector.getZ()));
            inputVector = new Vector(inputVector.getX() / d0, inputVector.getY() / d0, inputVector.getZ() / d0);
        }

        return inputVector;
    }

    public void guessBestMovement(float speed, GrimPlayer player) {
        double bestInput = Double.MAX_VALUE;

        List<VectorData> possibleVelocities = applyInputsToVelocityPossibilities(player, fetchPossibleStartTickVectors(player), speed);

        // Determine if the player can make an input below 0.03
        player.couldSkipTick = false;

        if (!player.inVehicle) {
            double threshold = player.uncertaintyHandler.getZeroPointZeroThreeThreshold();

            if (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree) {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.getX() * data.vector.getX() + data.vector.getZ() * data.vector.getZ() < threshold;
            } else {
                for (VectorData data : possibleVelocities)
                    player.couldSkipTick = player.couldSkipTick || data.vector.lengthSquared() < threshold;
            }
        }

        if (player.couldSkipTick) {
            Set<VectorData> zeroStuff = new HashSet<>();
            if (player.uncertaintyHandler.controlsVerticalMovement())
                zeroStuff.add(new VectorData(new Vector(), VectorData.VectorType.ZeroPointZeroThree));
            else
                zeroStuff.add(new VectorData(new Vector().setY(player.clientVelocity.getY()), VectorData.VectorType.ZeroPointZeroThree));
            addJumpsToPossibilities(player, zeroStuff);
            possibleVelocities.addAll(applyInputsToVelocityPossibilities(player, zeroStuff, speed));

            double yVelocity = player.clientVelocity.getY();

            if (Math.abs(yVelocity) < 0.03) {
                yVelocity -= 0.08;

                // damn swimming on water
                if (player.actualMovement.getY() < -0.08) {
                    if (player.compensatedWorld.containsLiquid(player.boundingBox.copy().offset(0, -0.1, 0))) {
                        yVelocity -= 0.16;
                    }
                }

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
                //
                // 0.001 was causing issues with horizontal collision resulting in 1e-4 (which should flag checks!)
                // Ladders are the best way to see this behavior
                // Remember this is squared so it is actually 0.00001
                //
                // This should likely be the value for the predictions to flag the movement as invalid
                if (resultAccuracy < 0.00001 * 0.00001) break;
            }
        }

        // The player always has at least one velocity - clientVelocity
        assert bestCollisionVel != null;
        player.clientVelocity = tempClientVelChosen;
        new MovementTickerPlayer(player).move(beforeCollisionMovement, bestCollisionVel.vector);
        player.predictedVelocity = bestCollisionVel;
        endOfTick(player, player.gravity, player.friction);
    }

    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        Set<VectorData> velocities = player.getPossibleVelocities();

        addExplosionRiptideToPossibilities(player, velocities);
        addJumpsToPossibilities(player, velocities);

        return velocities;
    }

    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
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

        return Double.compare(a.vector.distanceSquared(player.actualMovement), b.vector.distanceSquared(player.actualMovement));
    }

    private Vector handleStartingVelocityUncertainty(GrimPlayer player, VectorData vector) {
        double avgColliding = GrimMathHelper.calculateAverage(player.uncertaintyHandler.strictCollidingEntities);

        double additionHorizontal = player.uncertaintyHandler.getOffsetHorizontal(vector);
        double additionVertical = player.uncertaintyHandler.getVerticalOffset(vector);

        // Gliding status changed, there are a decent amount of edge cases in this scenario so give lenience
        if (player.isGliding != player.wasGliding)
            additionHorizontal += 0.05;

        // ViaVersion playing with flight speed causes a bug on 1.7 clients while exiting flying
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && player.wasFlying)
            additionHorizontal += 0.05;


        double uncertainPiston = 0;
        for (int x = 0; x < player.uncertaintyHandler.pistonPushing.size(); x++) {
            double value = player.uncertaintyHandler.pistonPushing.get(x);
            if (value == 0) continue;
            value *= (Math.pow(0.8, x));
            uncertainPiston = Math.max(uncertainPiston, value);
        }

        // "temporary" workaround for when player enters flight from gliding
        double bonusY = 0;
        if (Collections.max(player.uncertaintyHandler.tempElytraFlightHack)) {
            additionHorizontal += 0.1;
            bonusY += 0.1;
        }

        Vector uncertainty = new Vector(avgColliding * 0.04 + uncertainPiston, additionVertical + uncertainPiston, avgColliding * 0.04 + uncertainPiston);
        Vector min = new Vector(player.uncertaintyHandler.xNegativeUncertainty - additionHorizontal, -bonusY + player.uncertaintyHandler.yNegativeUncertainty + player.uncertaintyHandler.gravityUncertainty, player.uncertaintyHandler.zNegativeUncertainty - additionHorizontal);
        Vector max = new Vector(player.uncertaintyHandler.xPositiveUncertainty + additionHorizontal, bonusY + player.uncertaintyHandler.yPositiveUncertainty + (player.uncertaintyHandler.lastLastPacketWasGroundPacket || player.uncertaintyHandler.isSteppingOnSlime ? 0.03 : 0), player.uncertaintyHandler.zPositiveUncertainty + additionHorizontal);

        Vector minVector = vector.vector.clone().add(min.subtract(uncertainty));
        Vector maxVector = vector.vector.clone().add(max.add(uncertainty));

        // Player velocity can multiply 0.4-0.45 (guess on max) when the player is on slime with
        // a Y velocity of 0 to 0.1.  Because 0.03 we don't know this so just give lenience here
        if (player.uncertaintyHandler.isSteppingOnSlime) {
            if (vector.vector.getX() > 0) {
                minVector.multiply(new Vector(0.4, 1, 1));
            } else {
                maxVector.multiply(new Vector(0.4, 1, 1));
            }

            if (vector.vector.getZ() > 0) {
                minVector.multiply(new Vector(1, 1, 0.4));
            } else {
                maxVector.multiply(new Vector(1, 1, 0.4));
            }

            // If the player is using fireworks on slime
            // Their Y velocity gets hidden once by fireworks applying push movement
            // Then again by bouncing on the slime itself
            // Then again by 0.03
            // Give up, what cheat could exploit slime and fireworks?
            if (player.compensatedFireworks.getMaxFireworksAppliedPossible() > 0) {
                minVector.setY(0);
            }
        }

        if ((player.uncertaintyHandler.wasLastOnGroundUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket) && vector.vector.getY() < 0) {
            maxVector.setY(0);
        }

        // ViaVersion playing with flight speed causes a bug on 1.7 clients while exiting flying
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && player.wasFlying)
            minVector.setY(0);

        return PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement, minVector, maxVector);
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
            speed -= speed * 0.3F;
            loopVectors(player, possibleVectors, speed, returnVectors);
            player.isSprinting = true;
        }

        return returnVectors;
    }

    private void loopVectors(GrimPlayer player, Set<VectorData> possibleVectors, float speed, List<VectorData> returnVectors) {
        // Stop omni-sprint
        // Optimization - Also cuts down scenarios by 2/3
        // For some reason the player sprints while swimming no matter what
        // Probably as a way to tell the server it is swimming
        int zMin = player.isSprinting && !player.isSwimming ? 1 : -1;

        AlmostBoolean usingItem = player.isUsingItem;
        boolean loopAgain = true;

        // Loop twice for the using item status if the player is using a trident
        // (Or in the future mojang desync's with another item and we can't be sure)
        for (int loopUsingItem = 0; loopAgain && loopUsingItem <= 1; loopUsingItem++) {
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

            // Loop again if the player is using a riptide trident in the rain (as this is too easy to desync)
            if (loopAgain = (player.isUsingItem == AlmostBoolean.MAYBE)) {
                player.isUsingItem = AlmostBoolean.FALSE;
            }
        }

        player.isUsingItem = usingItem;
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

    public boolean canSwimHop(GrimPlayer player) {
        if (player.inVehicle)
            return false;

        boolean canCollideHorizontally = !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -0.01, 0.5));

        if (!canCollideHorizontally)
            return false;

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

        return player.compensatedWorld.containsLiquid(player.boundingBox.copy().expand(0.1, 0.1, 0.1));
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

    public void endOfTick(GrimPlayer player, double d, float friction) {
        player.canSwimHop = canSwimHop(player);
        player.lastWasClimbing = 0;
    }

    public Vector handleFireworkMovementLenience(GrimPlayer player, Vector vector) {
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

        Vector cutOne = PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement, boostOne, vector);
        Vector cutTwo = PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement, boostTwo, vector);

        return PredictionEngineElytra.cutVectorsToPlayerMovement(player.actualMovement, cutOne, cutTwo);
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
