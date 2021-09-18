package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import ac.grim.grimac.utils.nmsImplementations.Riptide;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictionEngine {

    public void guessBestMovement(float speed, GrimPlayer player) {
        List<VectorData> possibleVelocities = applyInputsToVelocityPossibilities(player, fetchPossibleStartTickVectors(player), speed);

        // Determine if the player can make an input below 0.03
        player.couldSkipTick = player.uncertaintyHandler.canSkipTick(possibleVelocities);

        if (player.couldSkipTick) {
            Set<VectorData> zeroStuff = new HashSet<>();

            // Allow the player's Y velocity to be 0 if they are in water/lava (0.03 issue)
            Vector pointThreeVector = new Vector();
            if (!player.uncertaintyHandler.controlsVerticalMovement())
                pointThreeVector.setY(player.clientVelocity.getY() * player.stuckSpeedMultiplier.getY());

            VectorData zeroData = new VectorData(pointThreeVector, VectorData.VectorType.ZeroPointZeroThree);
            zeroStuff.add(zeroData);

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && player.isSwimming) {
                zeroStuff = PredictionEngineWater.transformSwimmingVectors(player, zeroStuff);
            }

            Set<VectorData> jumpingPossibility = new HashSet<>();
            jumpingPossibility.add(new VectorData(new Vector(), VectorData.VectorType.ZeroPointZeroThree));

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && player.isSwimming) {
                jumpingPossibility = PredictionEngineWater.transformSwimmingVectors(player, jumpingPossibility);
            }

            addJumpsToPossibilities(player, jumpingPossibility);
            // Secure the ability to get predicted a new vector by forcing the player to be able to jump here
            // Adding jumps to possibilities is a secure method
            if (jumpingPossibility.size() > 1) {
                zeroStuff.addAll(jumpingPossibility);
            }

            addExplosionRiptideToPossibilities(player, zeroStuff);
            possibleVelocities.addAll(applyInputsToVelocityPossibilities(player, zeroStuff, speed));

            double yVelocity = player.clientVelocity.getY();

            // Vertical 0.03 where you collide upwards into a block
            // TRAPDOOR
            // AIR
            // AIR
            // STONE
            // Jump from the stone into the trapdoor, and you just collided with the trapdoor in 0.03!
            if (yVelocity > 0 && !Collisions.isEmpty(player, player.boundingBox.copy().expand(-SimpleCollisionBox.COLLISION_EPSILON).offset(0, 0.03, 0))) {
                // Wow, this can really mess things up!
                // Allow the player's Y velocity to get set back to 0, minus the normal gravity uncertainty
                player.uncertaintyHandler.gravityUncertainty += (-yVelocity - 0.2);
            } else if ((player.firstBreadKB != null && Math.abs(player.firstBreadKB.vector.getY()) < 0.03)
                    || (player.likelyKB != null && Math.abs(player.likelyKB.vector.getY()) < 0.03)) {
                // If the player knockback was likely to cause 0.03 missing tick
                player.uncertaintyHandler.gravityUncertainty -= 0.2;
            } else if (Math.abs(yVelocity) < 0.03) {
                // Falses with -0.16
                player.uncertaintyHandler.gravityUncertainty -= 0.2;
            } else if (player.uncertaintyHandler.wasAffectedByStuckSpeed()) {
                player.uncertaintyHandler.gravityUncertainty -= 0.1;
            }
        }

        // Sorting is an optimization and a requirement
        possibleVelocities.sort((a, b) -> sortVectorData(a, b, player));

        double bestInput = Double.MAX_VALUE;
        VectorData bestCollisionVel = null;
        Vector beforeCollisionMovement = null;
        Vector tempClientVelChosen = null;
        Vector originalClientVel = player.clientVelocity;

        Pose originalPose = player.pose;
        SimpleCollisionBox originalBB = player.boundingBox;

        for (VectorData clientVelAfterInput : possibleVelocities) {
            Vector backOff = handleStartingVelocityUncertainty(player, clientVelAfterInput);
            Vector additionalPushMovement = handlePushMovementThatDoesntAffectNextTickVel(player, backOff);
            Vector primaryPushMovement = Collisions.maybeBackOffFromEdge(additionalPushMovement, player, false);

            boolean flipSneaking = clientVelAfterInput.isFlipSneaking();
            if (flipSneaking) {
                player.pose = originalPose == Pose.STANDING ? player.getSneakingPose() : Pose.STANDING;
                player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            } else {
                player.pose = originalPose;
                player.boundingBox = originalBB;
            }

            double xAdditional = (Math.signum(primaryPushMovement.getX()) * SimpleCollisionBox.COLLISION_EPSILON);
            double yAdditional = (player.hasGravity ? SimpleCollisionBox.COLLISION_EPSILON : 0);
            double zAdditional = (Math.signum(primaryPushMovement.getX()) * SimpleCollisionBox.COLLISION_EPSILON);

            // Expand by the collision epsilon to test if the player collided with a block (as this resets the velocity in that direction)
            double testX = primaryPushMovement.getX() + xAdditional;
            double testY = primaryPushMovement.getY() - yAdditional;
            double testZ = primaryPushMovement.getZ() + zAdditional;
            primaryPushMovement = new Vector(testX, testY, testZ);

            Vector bestTheoreticalCollisionResult = VectorUtils.cutBoxToVector(player.actualMovement, new SimpleCollisionBox(0, Math.min(0, testY), 0, testX, Math.max(0.6, testY), testZ).sort());
            if (bestTheoreticalCollisionResult.distanceSquared(player.actualMovement) > bestInput && !clientVelAfterInput.isKnockback() && !clientVelAfterInput.isExplosion())
                continue;

            Vector outputVel = Collisions.collide(player, primaryPushMovement.getX(), primaryPushMovement.getY(), primaryPushMovement.getZ(), originalClientVel.getY());

            if (testX == outputVel.getX()) { // the player didn't have X collision, don't ruin offset by collision epsilon
                primaryPushMovement.setX(primaryPushMovement.getX() - xAdditional);
                outputVel.setX(outputVel.getX() - xAdditional);
            }

            if (testY == outputVel.getY()) { // the player didn't have Y collision, don't ruin offset by collision epsilon
                primaryPushMovement.setY(primaryPushMovement.getY() + yAdditional);
                outputVel.setY(outputVel.getY() + yAdditional);
            }

            if (testZ == outputVel.getZ()) { // the player didn't have Z collision, don't ruin offset by collision epsilon
                primaryPushMovement.setZ(primaryPushMovement.getZ() - zAdditional);
                outputVel.setZ(outputVel.getZ() - zAdditional);
            }

            Vector handleHardCodedBorder = outputVel;
            if (!player.inVehicle) {
                double d0 = GrimMath.clamp(player.lastX + outputVel.getX(), -2.9999999E7D, 2.9999999E7D);
                double d1 = GrimMath.clamp(player.lastZ + outputVel.getZ(), -2.9999999E7D, 2.9999999E7D);
                if (d0 != player.lastX + handleHardCodedBorder.getX()) {
                    handleHardCodedBorder = new Vector(d0 - player.lastX, handleHardCodedBorder.getY(), handleHardCodedBorder.getZ());
                }

                if (d1 != player.lastZ + handleHardCodedBorder.getZ()) {
                    handleHardCodedBorder = new Vector(handleHardCodedBorder.getX(), handleHardCodedBorder.getY(), d1 - player.lastZ);
                }
            }

            double resultAccuracy = handleHardCodedBorder.distanceSquared(player.actualMovement);

            // This allows us to always check the percentage of knockback taken
            // A player cannot simply ignore knockback without us measuring how off it was
            if (clientVelAfterInput.isKnockback())
                player.checkManager.getKnockbackHandler().handlePredictionAnalysis(Math.sqrt(resultAccuracy), handleHardCodedBorder);
            if (clientVelAfterInput.isExplosion())
                player.checkManager.getExplosionHandler().handlePredictionAnalysis(Math.sqrt(resultAccuracy), handleHardCodedBorder);

            if (resultAccuracy < bestInput) {
                bestCollisionVel = clientVelAfterInput.returnNewModified(outputVel, VectorData.VectorType.BestVelPicked);
                beforeCollisionMovement = primaryPushMovement;
                tempClientVelChosen = primaryPushMovement.clone();

                bestInput = resultAccuracy;

                // Optimization - Close enough, other inputs won't get closer
                // This works as knockback and explosions are run first
                //
                // Note that sometimes the first and closest velocity isn't the closest because collisions
                // The player may only be able to move a slight amount compared to what the initial vector shows
                //
                // 0.001 was causing issues with horizontal collision resulting in 1e-4 (which should flag checks!)
                // Ladders are the best way to see this behavior
                // Remember this is squared, so it is actually 0.00001
                //
                // This should likely be the value for the predictions to flag the movement as invalid
                if (resultAccuracy < 0.00001 * 0.00001) break;
            }
        }

        // The player always has at least one velocity - clientVelocity
        assert bestCollisionVel != null;

        boolean flipSneaking = bestCollisionVel.isFlipSneaking();
        if (flipSneaking) {
            player.pose = originalPose == Pose.STANDING ? player.getSneakingPose() : Pose.STANDING;
            player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
        } else {
            player.pose = originalPose;
            player.boundingBox = originalBB;
        }

        player.clientVelocity = tempClientVelChosen;
        player.predictedVelocity = bestCollisionVel; // Set predicted vel to get the vector types later in the move method
        new MovementTickerPlayer(player).move(beforeCollisionMovement, bestCollisionVel.vector);
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
            // Flying with sprinting increases speed by 2x
            if (player.isFlying)
                speed -= speed / 2;
            else
                speed /= 1.3f;
            loopVectors(player, possibleVectors, speed, returnVectors);
            player.isSprinting = true;
        }

        return returnVectors;
    }

    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        Set<VectorData> velocities = player.getPossibleVelocities();

        addExplosionRiptideToPossibilities(player, velocities);

        Set<VectorData> velocitiesToReturn = new HashSet<>(velocities);

        for (int x = 1; x <= player.maxPlayerAttackSlow; x++) {
            for (VectorData data : new HashSet<>(velocitiesToReturn)) {
                velocitiesToReturn.add(data.returnNewModified(data.vector.clone().multiply(new Vector(0.6, 1, 0.6)), VectorData.VectorType.AttackSlow));
            }
        }

        addJumpsToPossibilities(player, velocitiesToReturn);

        return velocitiesToReturn;
    }

    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
    }

    public void addExplosionRiptideToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.likelyExplosions != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.likelyExplosions.vector), vector, VectorData.VectorType.Explosion));
            }

            if (player.firstBreadExplosion != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.firstBreadExplosion.vector), vector, VectorData.VectorType.Explosion));
            }
        }

        if (player.tryingToRiptide) {
            Vector riptideAddition = Riptide.getRiptideVelocity(player);

            existingVelocities.add(new VectorData(player.clientVelocity.clone().add(riptideAddition), VectorData.VectorType.Trident));
        }
    }

    public int sortVectorData(VectorData a, VectorData b, GrimPlayer player) {
        int aScore = 0;
        int bScore = 0;

        // Fixes false using riptide under 2 blocks of water
        boolean aTridentJump = a.isTrident() && !a.isJump();
        boolean bTridentJump = b.isTrident() && !b.isJump();

        if (aTridentJump && !bTridentJump)
            return -1;

        if (bTridentJump && !aTridentJump)
            return 1;

        // Put explosions and knockback first so they are applied to the player
        // Otherwise the anticheat can't handle minor knockback and explosions without knowing if the player took the kb
        if (a.isExplosion())
            aScore--;

        if (a.isKnockback())
            aScore--;

        if (b.isExplosion())
            bScore--;

        if (b.isKnockback())
            bScore--;

        // If the player is on the ground but the vector leads the player off the ground
        if (player.onGround && a.vector.getY() >= 0)
            aScore += 2;

        if (player.onGround && b.vector.getY() >= 0)
            bScore += 2;

        if (aScore != bScore)
            return Integer.compare(aScore, bScore);

        return Double.compare(a.vector.distanceSquared(player.actualMovement), b.vector.distanceSquared(player.actualMovement));
    }

    private Vector handleStartingVelocityUncertainty(GrimPlayer player, VectorData vector) {
        double avgColliding = GrimMath.calculateAverage(player.uncertaintyHandler.collidingEntities);

        double additionHorizontal = player.uncertaintyHandler.getOffsetHorizontal(vector);
        double additionVertical = player.uncertaintyHandler.getVerticalOffset(vector);

        additionHorizontal += player.uncertaintyHandler.lastHorizontalOffset;
        additionVertical += player.uncertaintyHandler.lastVerticalOffset;

        if (player.playerVehicle instanceof PacketEntityRideable && player.vehicleData.lastVehicleSwitch < 5) {
            Vector3d playerPosition = player.playerVehicle.position;
            SimpleCollisionBox uncertainBox = new SimpleCollisionBox(playerPosition, playerPosition);
            for (Vector3d possiblePosition : ((PacketEntityRideable) player.playerVehicle).entityPositions) {
                uncertainBox.expandToAbsoluteCoordinates(possiblePosition.getX(), possiblePosition.getY(), possiblePosition.getZ());
            }

            player.uncertaintyHandler.xNegativeUncertainty -= playerPosition.getX() - uncertainBox.minX;
            player.uncertaintyHandler.zNegativeUncertainty -= playerPosition.getZ() - uncertainBox.minZ;
            player.uncertaintyHandler.yNegativeUncertainty -= playerPosition.getY() - uncertainBox.minY;
            player.uncertaintyHandler.yPositiveUncertainty -= playerPosition.getY() - uncertainBox.maxY;
            player.uncertaintyHandler.xPositiveUncertainty -= playerPosition.getX() - uncertainBox.maxX;
            player.uncertaintyHandler.zPositiveUncertainty -= playerPosition.getZ() - uncertainBox.maxZ;

            player.uncertaintyHandler.yNegativeUncertainty -= 0.5;
        }

        double uncertainPiston = 0;
        for (int x = 0; x < player.uncertaintyHandler.pistonPushing.size(); x++) {
            double value = player.uncertaintyHandler.pistonPushing.get(x);
            if (value == 0) continue;
            value *= (Math.pow(0.8, x));
            uncertainPiston = Math.max(uncertainPiston, value);
        }

        // "temporary" workaround for when player toggles flight
        // Difficult as there are a ton of edge cases and version differences with flying
        // For example, try toggling not using elytra to flying without this hack
        double bonusY = 0;
        if (player.uncertaintyHandler.lastFlyingStatusChange > -5) {
            additionHorizontal += 0.3;
            bonusY += 0.3;
        }

        if (player.uncertaintyHandler.lastUnderwaterFlyingHack > -10) {
            bonusY += 0.2;
        }

        if (player.uncertaintyHandler.lastHardCollidingLerpingEntity > -3) {
            additionHorizontal += 0.1;
            bonusY += 0.1;
        }

        Vector uncertainty = new Vector(avgColliding * 0.04 + uncertainPiston, additionVertical + uncertainPiston, avgColliding * 0.04 + uncertainPiston);
        Vector min = new Vector(player.uncertaintyHandler.xNegativeUncertainty - additionHorizontal, -bonusY + player.uncertaintyHandler.yNegativeUncertainty + player.uncertaintyHandler.gravityUncertainty, player.uncertaintyHandler.zNegativeUncertainty - additionHorizontal);
        Vector max = new Vector(player.uncertaintyHandler.xPositiveUncertainty + additionHorizontal, bonusY + player.uncertaintyHandler.yPositiveUncertainty + (player.uncertaintyHandler.lastLastPacketWasGroundPacket ? 0.03 : 0), player.uncertaintyHandler.zPositiveUncertainty + additionHorizontal);

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

        // Initial end of tick levitation gets hidden by missing idle packet
        if (player.levitationAmplifier > 0 && player.clientVelocity.getY() < 0.1) {
            maxVector.setY(maxVector.getY() + 0.1);
        }

        // Handle 0.03 with fluid pushing players downwards
        if (player.baseTickAddition.getY() < 0 && player.wasTouchingWater &&
                (vector.isZeroPointZeroThree() || vector.isSwimHop())) {
            minVector.setY(minVector.getY() + player.baseTickAddition.getY());
        }

        // Handle 0.03 with fluid pushing players upwards (the player moved out of the pushing inside 0.03 movement)
        if (player.couldSkipTick && player.baseTickAddition.getY() > 0 && player.wasTouchingWater &&
                (vector.isZeroPointZeroThree() || vector.isSwimHop())) {
            maxVector.setY(maxVector.getY() + player.baseTickAddition.getY());
        }

        if (player.actualMovement.getY() > 0 && player.uncertaintyHandler.influencedByBouncyBlock()) {
            double slimeBlockBounce = Math.max(Math.abs(player.uncertaintyHandler.slimeBlockUpwardsUncertainty.get(0)), Math.abs(player.uncertaintyHandler.slimeBlockUpwardsUncertainty.get(1)));
            if (slimeBlockBounce != 0) {
                if (slimeBlockBounce > maxVector.getY()) maxVector.setY(slimeBlockBounce);
                if (minVector.getY() > 0) minVector.setY(0);
            }
        }

        return VectorUtils.cutBoxToVector(player.actualMovement, minVector, maxVector);
    }

    public Vector handlePushMovementThatDoesntAffectNextTickVel(GrimPlayer player, Vector vector) {
        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        double avgColliding = GrimMath.calculateAverage(player.uncertaintyHandler.collidingEntities);

        // 0.03 was falsing when colliding with https://i.imgur.com/7obfxG6.png
        // 0.065 was causing issues with fast moving dolphins
        // 0.075 seems safe?
        //
        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        Vector uncertainty = new Vector(player.uncertaintyHandler.pistonX + avgColliding * 0.075, player.uncertaintyHandler.pistonY, player.uncertaintyHandler.pistonZ + avgColliding * 0.075);
        return VectorUtils.cutBoxToVector(player.actualMovement,
                vector.clone().add(uncertainty.clone().multiply(-1)).add(new Vector(0, player.uncertaintyHandler.wasLastOnGroundUncertain ? -0.03 : 0, 0)),
                vector.clone().add(uncertainty));
    }

    public void endOfTick(GrimPlayer player, double d, float friction) {
        player.canSwimHop = canSwimHop(player);
        player.lastWasClimbing = 0;
    }

    private void loopVectors(GrimPlayer player, Set<VectorData> possibleVectors, float speed, List<VectorData> returnVectors) {
        // Stop omni-sprint
        // Optimization - Also cuts down scenarios by 2/3
        // For some reason the player sprints while swimming no matter what
        // Probably as a way to tell the server it is swimming
        int zMin = player.isSprinting && !player.isSwimming ? 1 : -1;

        AlmostBoolean usingItem = player.isUsingItem;
        boolean loopCrouching = true;

        // Loop twice for the using item status if the player is using a trident
        // (Or in the future mojang desync's with another item and we can't be sure)
        //
        // I tried using delays, vertical collision detection, and other methods for sneaking
        // But nothing works as well as brute force
        for (int loopSneaking = 0; loopCrouching && loopSneaking <= 1; loopSneaking++) {
            boolean loopAgain = true;
            for (int loopUsingItem = 0; loopAgain && loopUsingItem <= 1; loopUsingItem++) {
                for (VectorData possibleLastTickOutput : possibleVectors) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = zMin; z <= 1; z++) {
                            VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(getMovementResultFromInput(player, transformInputsToVector(player, new Vector(x, 0, z)), speed, player.xRot)), possibleLastTickOutput, VectorData.VectorType.InputResult);
                            result = result.returnNewModified(handleFireworkMovementLenience(player, result.vector.clone()), VectorData.VectorType.Lenience);
                            result = result.returnNewModified(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
                            result = result.returnNewModified(handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
                            // Signal that we need to flip sneaking bounding box
                            if (loopSneaking == 1)
                                result = result.returnNewModified(result.vector, VectorData.VectorType.Flip_Sneaking);
                            returnVectors.add(result);
                        }
                    }
                }


                // Loop again if the player is using a riptide trident in the rain (as this is too easy to desync)
                loopAgain = (player.isUsingItem == AlmostBoolean.MAYBE);
                if (loopAgain) {
                    player.isUsingItem = AlmostBoolean.FALSE;
                }
            }

            // The client has this stupid sneaking mechanic that desync's from the server because mojang
            // is incompetent at modern netcode
            // The player changed their sneaking within 3 ticks of this
            // And the player's pose is standing or crouching (no gliding/swimming)
            // Falses when set to -4, so therefore set to -5
            //
            // Also desync's because 1.9-1.13 lack of idle packet... NICE ONE MOJANG!
            // Also less terrible because it only desync's for one tick instead of a large number of ticks...
            loopCrouching = ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) &&
                    player.uncertaintyHandler.lastSneakingChangeTicks > -5)

                    || player.getClientVersion().isOlderThan(ClientVersion.v_1_14) &&
                    player.uncertaintyHandler.lastSneakingChangeTicks > -1) &&

                    (player.pose == Pose.STANDING || player.pose == Pose.CROUCHING || player.pose == Pose.NINE_CROUCHING);

            if (loopCrouching) {
                player.isCrouching = !player.isCrouching;
                player.isSlowMovement = !player.isSlowMovement;
                player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            }
        }

        if (loopCrouching) {
            player.isCrouching = !player.isCrouching;
            player.isSlowMovement = !player.isSlowMovement;
            player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
        }

        player.isUsingItem = usingItem;
    }

    public boolean canSwimHop(GrimPlayer player) {
        // Boats cannot swim hop, all other living entities should be able to.
        if (player.playerVehicle != null && player.playerVehicle.type == EntityType.BOAT)
            return false;

        // This uses the new bounding box
        boolean canCollideHorizontally = !Collisions.isEmpty(player, GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6, 1.8).expand(
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

        SimpleCollisionBox oldBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.6, 1.8);

        // This uses the old bounding box
        // (Water/lava checked before movement)
        return player.compensatedWorld.containsLiquid(oldBox.expand(0.1, 0.1, 0.1));
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

    public Vector handleFireworkMovementLenience(GrimPlayer player, Vector vector) {
        int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;

        if (maxFireworks <= 0) return vector;
        if (!player.isGliding && !player.wasGliding) return vector;

        Vector currentLook = PredictionEngineElytra.getVectorForRotation(player, player.yRot, player.xRot);
        Vector lastLook = PredictionEngineElytra.getVectorForRotation(player, player.lastYRot, player.lastXRot);

        Vector boostOne = vector.clone();
        Vector boostTwo = vector.clone();

        for (int i = 0; i < maxFireworks; i++) {
            boostOne.add(new Vector(currentLook.getX() * 0.1 + (currentLook.getX() * 1.5 - boostOne.getX()) * 0.5, currentLook.getY() * 0.1 + (currentLook.getY() * 1.5 - boostOne.getY()) * 0.5, (currentLook.getZ() * 0.1 + (currentLook.getZ() * 1.5 - boostOne.getZ()) * 0.5)));
            boostTwo.add(new Vector(lastLook.getX() * 0.1 + (lastLook.getX() * 1.5 - boostTwo.getX()) * 0.5, lastLook.getY() * 0.1 + (lastLook.getY() * 1.5 - boostTwo.getY()) * 0.5, (lastLook.getZ() * 0.1 + (lastLook.getZ() * 1.5 - boostTwo.getZ()) * 0.5)));
        }

        Vector cutOne = VectorUtils.cutBoxToVector(player.actualMovement, boostOne, vector);
        Vector cutTwo = VectorUtils.cutBoxToVector(player.actualMovement, boostTwo, vector);

        return VectorUtils.cutBoxToVector(player.actualMovement, cutOne, cutTwo);
    }

    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        return vector;
    }

    public void doJump(GrimPlayer player, Vector vector) {
        if (!player.lastOnGround || player.onGround)
            return;

        JumpPower.jumpFromGround(player, vector);
    }
}
