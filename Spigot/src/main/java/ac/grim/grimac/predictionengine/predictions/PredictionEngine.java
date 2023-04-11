package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import ac.grim.grimac.utils.nmsutil.Riptide;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

import java.util.*;

public class PredictionEngine {

    public static Vector clampMovementToHardBorder(GrimPlayer player, Vector outputVel) {
        // TODO: Reimplement
        return outputVel;
    }

    public static Vector transformInputsToVector(GrimPlayer player, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        // Slow movement was determined by the previous pose
        if (player.isSlowMovement) {
            bestPossibleX = (float) (theoreticalInput.getX() * player.sneakingSpeedMultiplier);
            bestPossibleZ = (float) (theoreticalInput.getZ() * player.sneakingSpeedMultiplier);
        } else {
            bestPossibleX = Math.min(Math.max(-1f, Math.round(theoreticalInput.getX())), 1f);
            bestPossibleZ = Math.min(Math.max(-1f, Math.round(theoreticalInput.getZ())), 1f);
        }

        if (player.packetStateData.slowedByUsingItem) {
            bestPossibleX *= 0.2F;
            bestPossibleZ *= 0.2F;
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);
        inputVector.multiply(0.98F);

        // Simulate float rounding imprecision
        inputVector = new Vector((float) inputVector.getX(), (float) inputVector.getY(), (float) inputVector.getZ());

        if (inputVector.lengthSquared() > 1) {
            double d0 = Math.sqrt(inputVector.getX() * inputVector.getX() + inputVector.getY() * inputVector.getY() + inputVector.getZ() * inputVector.getZ());
            inputVector = new Vector(inputVector.getX() / d0, inputVector.getY() / d0, inputVector.getZ() / d0);
        }

        return inputVector;
    }

    public void guessBestMovement(float speed, GrimPlayer player) {
        Set<VectorData> init = fetchPossibleStartTickVectors(player);

        if (player.uncertaintyHandler.influencedByBouncyBlock()) {
            for (VectorData data : init) {
                // Try to get the vector as close to zero as possible to give the best chance at 0.03...
                Vector toZeroVec = new PredictionEngine().handleStartingVelocityUncertainty(player, data, new Vector(0, -1000000000, 0)); // Downwards without overflow risk

                player.uncertaintyHandler.nextTickSlimeBlockUncertainty = Math.max(Math.abs(toZeroVec.getY()), player.uncertaintyHandler.nextTickSlimeBlockUncertainty);
            }
        }

        player.updateVelocityMovementSkipping();
        player.couldSkipTick = player.couldSkipTick || player.pointThreeEstimator.determineCanSkipTick(speed, init);

        // Remember, we must always try to predict explosions or knockback
        // If the player didn't skip their tick... then we can do predictions
        //
        // Although this may lead to bypasses, it will be better to just use the predictions
        // which sustain the last player's tick speed...
        // Nothing in the air can really be skipped, so that's off the table (flight, actual knockback, etc)
        //
        // Remember, we don't have to detect 100% of cheats, if the cheats we don't detect are a disadvantage
        // We didn't apply inputs yet because it would mess up the 0.03 detection!
        List<VectorData> possibleVelocities = applyInputsToVelocityPossibilities(player, init, speed);

        // If the player took knockback or explosions, we must do predictions to check their offset
        if (player.couldSkipTick) {
            addZeroPointThreeToPossibilities(speed, player, possibleVelocities);
        }

        // Finally, this was not 0.03 or small movements, so we can attempt to predict it.
        doPredictions(player, possibleVelocities, speed);

        // Client velocity - before collision and carried into the next tick
        // Predicted velocity - after collision and not carried into the next tick
        new MovementTickerPlayer(player).move(player.clientVelocity.clone(), player.predictedVelocity.vector);
        endOfTick(player, player.gravity);
    }

    private void doPredictions(GrimPlayer player, List<VectorData> possibleVelocities, float speed) {
        // Computers are actually really fast at sorting, I don't see sorting as a problem
        possibleVelocities.sort((a, b) -> sortVectorData(a, b, player));

        player.checkManager.getPostPredictionCheck(SneakingEstimator.class).storePossibleVelocities(possibleVelocities);

        double bestInput = Double.MAX_VALUE;

        VectorData bestCollisionVel = null;
        Vector beforeCollisionMovement = null;
        Vector originalClientVel = player.clientVelocity.clone();

        SimpleCollisionBox originalBB = player.boundingBox;
        // 0.03 doesn't exist with vehicles, thank god
        // 1.13+ clients have stupid poses that desync because mojang brilliantly removed the idle packet in 1.9
        SimpleCollisionBox pointThreeThanksMojang = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.6f, 0.6f) : originalBB;

        player.skippedTickInActualMovement = false;

        for (VectorData clientVelAfterInput : possibleVelocities) {
            Vector primaryPushMovement = handleStartingVelocityUncertainty(player, clientVelAfterInput, player.actualMovement);

            Vector bestTheoreticalCollisionResult = VectorUtils.cutBoxToVector(player.actualMovement, new SimpleCollisionBox(0, Math.min(0, primaryPushMovement.getY()), 0, primaryPushMovement.getX(), Math.max(0.6, primaryPushMovement.getY()), primaryPushMovement.getZ()).sort());
            // Check if this vector could ever possible beat the last vector in terms of accuracy
            // This is quite a good optimization :)
            if (bestTheoreticalCollisionResult.distanceSquared(player.actualMovement) > bestInput && !clientVelAfterInput.isKnockback() && !clientVelAfterInput.isExplosion()) {
                continue;
            }

            if (clientVelAfterInput.isZeroPointZeroThree()) {
                player.boundingBox = pointThreeThanksMojang;
            } else {
                player.boundingBox = originalBB;
            }

            // Returns pair of primary push movement, and then outputvel
            Pair<Vector, Vector> output = doSeekingWallCollisions(player, primaryPushMovement, originalClientVel, clientVelAfterInput);
            primaryPushMovement = output.getFirst();
            Vector outputVel = clampMovementToHardBorder(player, output.getSecond());

            double resultAccuracy = outputVel.distanceSquared(player.actualMovement);

            // Check if this possiblity is zero point zero three and is "close enough" to the player's actual movement
            if (clientVelAfterInput.isZeroPointZeroThree() && resultAccuracy < 0.001 * 0.001) {
                player.skippedTickInActualMovement = true;
            }

            if (clientVelAfterInput.isKnockback()) {
                player.checkManager.getKnockbackHandler().handlePredictionAnalysis(Math.sqrt(player.uncertaintyHandler.reduceOffset(resultAccuracy)));
            }

            if (clientVelAfterInput.isExplosion()) {
                player.checkManager.getExplosionHandler().handlePredictionAnalysis(Math.sqrt(player.uncertaintyHandler.reduceOffset(resultAccuracy)));
            }

            // This allows us to always check the percentage of knockback taken
            // A player cannot simply ignore knockback without us measuring how off it was
            //
            // Exempt if the player
            if ((clientVelAfterInput.isKnockback() || clientVelAfterInput.isExplosion()) && !clientVelAfterInput.isZeroPointZeroThree()) {
                boolean wasVelocityPointThree = player.pointThreeEstimator.determineCanSkipTick(speed, new HashSet<>(Collections.singletonList(clientVelAfterInput)));

                if (clientVelAfterInput.isKnockback()) {
                    player.checkManager.getKnockbackHandler().setPointThree(wasVelocityPointThree);
                }
                if (clientVelAfterInput.isExplosion()) {
                    player.checkManager.getExplosionHandler().setPointThree(wasVelocityPointThree);
                }
            }

            // Whatever, if someone uses phase or something they will get caught by everything else...
            // Unlike knockback/explosions, there is no reason to force collisions to run to check it.
            // As not flipping item is preferred... it gets ran before any other options
            if (player.packetStateData.slowedByUsingItem && !clientVelAfterInput.isFlipItem()) {
                player.checkManager.getNoSlow().handlePredictionAnalysis(Math.sqrt(player.uncertaintyHandler.reduceOffset(resultAccuracy)));
            }

            if (player.checkManager.getKnockbackHandler().shouldIgnoreForPrediction(clientVelAfterInput) ||
                    player.checkManager.getExplosionHandler().shouldIgnoreForPrediction(clientVelAfterInput)) {
                continue;
            }

            if (resultAccuracy < bestInput) {
                bestCollisionVel = clientVelAfterInput.returnNewModified(outputVel, VectorData.VectorType.BestVelPicked);
                bestCollisionVel.preUncertainty = clientVelAfterInput;
                beforeCollisionMovement = primaryPushMovement;

                // We basically want to avoid falsing ground spoof, try to find a vector that works
                if (player.wouldCollisionResultFlagGroundSpoof(primaryPushMovement.getY(), bestCollisionVel.vector.getY()))
                    resultAccuracy += 0.0001 * 0.0001;

                bestInput = resultAccuracy;
            }

            // Close enough, there's no reason to continue our predictions (if either kb or explosion will flag, continue searching)
            if (bestInput < 1e-5 * 1e-5 && !player.checkManager.getKnockbackHandler().wouldFlag() && !player.checkManager.getExplosionHandler().wouldFlag()) {
                break;
            }
        }

        assert beforeCollisionMovement != null;

        player.clientVelocity = beforeCollisionMovement.clone();
        player.predictedVelocity = bestCollisionVel; // Set predicted vel to get the vector types later in the move method
        player.boundingBox = originalBB;

        // If the closest vector is 0.03, consider it 0.03.
        if (player.predictedVelocity.isZeroPointZeroThree()) {
            player.skippedTickInActualMovement = true;
        }
    }

    private Pair<Vector, Vector> doSeekingWallCollisions(GrimPlayer player, Vector primaryPushMovement, Vector originalClientVel, VectorData clientVelAfterInput) {
        boolean vehicleKB = player.compensatedEntities.getSelf().inVehicle() && clientVelAfterInput.isKnockback() && clientVelAfterInput.vector.getY() == 0;
        // Extra collision epsilon required for vehicles to be accurate
        double xAdditional = Math.signum(primaryPushMovement.getX()) * SimpleCollisionBox.COLLISION_EPSILON;
        // The server likes sending y=0 kb "lifting" the player off the ground.
        // The client doesn't send the vehicles onGround status, so we can't check for ground like normal.
        double yAdditional = vehicleKB ? 0 : (primaryPushMovement.getY() > 0 ? 1 : -1) * SimpleCollisionBox.COLLISION_EPSILON;
        double zAdditional = Math.signum(primaryPushMovement.getZ()) * SimpleCollisionBox.COLLISION_EPSILON;

        // Expand by the collision epsilon to test if the player collided with a block (as this resets the velocity in that direction)
        double testX = primaryPushMovement.getX() + xAdditional;
        double testY = primaryPushMovement.getY() + yAdditional;
        double testZ = primaryPushMovement.getZ() + zAdditional;
        primaryPushMovement = new Vector(testX, testY, testZ);

        Vector outputVel = Collisions.collide(player, primaryPushMovement.getX(), primaryPushMovement.getY(), primaryPushMovement.getZ(), originalClientVel.getY(), clientVelAfterInput);

        if (testX == outputVel.getX()) { // the player didn't have X collision, don't ruin offset by collision epsilon
            primaryPushMovement.setX(primaryPushMovement.getX() - xAdditional);
            outputVel.setX(outputVel.getX() - xAdditional);
        }

        if (testY == outputVel.getY()) { // the player didn't have Y collision, don't ruin offset by collision epsilon
            primaryPushMovement.setY(primaryPushMovement.getY() - yAdditional);
            outputVel.setY(outputVel.getY() - yAdditional);
        }

        if (testZ == outputVel.getZ()) { // the player didn't have Z collision, don't ruin offset by collision epsilon
            primaryPushMovement.setZ(primaryPushMovement.getZ() - zAdditional);
            outputVel.setZ(outputVel.getZ() - zAdditional);
        }

        return new Pair<>(primaryPushMovement, outputVel);
    }

    // 0.03 has some quite bad interactions with velocity + explosions (one extremely stupid line of code... thanks mojang)
    private void addZeroPointThreeToPossibilities(float speed, GrimPlayer player, List<VectorData> possibleVelocities) {
        Set<VectorData> pointThreePossibilities = new HashSet<>();

        // For now just let the player control their Y velocity within 0.03.  Gravity should stop exploits.
        // 0.03 - 0.784 < -0.03 = can't skip next tick
        Vector pointThreeVector = new Vector();

        // Stop a bypass (and fix falses) by carrying over the player's current velocity IF they couldn't have modified it
        if (!player.pointThreeEstimator.controlsVerticalMovement()) {
            pointThreeVector.setY(player.clientVelocity.getY());
        } else { // Carry over the current Y velocity to try and help with gravity issues
            pointThreePossibilities.add(new VectorData(new Vector(0, player.clientVelocity.getY(), 0), VectorData.VectorType.ZeroPointZeroThree));
        }

        pointThreePossibilities.add(new VectorData(pointThreeVector, VectorData.VectorType.ZeroPointZeroThree));

        // Swim hop
        if (player.pointThreeEstimator.isNearFluid && !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.4, 0, 0.4)) && !player.onGround) { // onGround can still be used here, else generic 0.03
            pointThreePossibilities.add(new VectorData(new Vector(0, 0.3, 0), VectorData.VectorType.ZeroPointZeroThree));
        }

        // Swimming vertically can add more Y velocity than normal
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && player.isSwimming) {
            pointThreePossibilities = PredictionEngineWater.transformSwimmingVectors(player, pointThreePossibilities);
        }

        // This is WRONG! Vanilla has this system at the end
        // However, due to 1.9 reduced movement precision, we aren't informed that the player could have this velocity
        // We still do climbing at the end, as it uses a different client velocity
        //
        // Force 1.13.2 and below players to have something to collide with horizontally to climb
        if (player.pointThreeEstimator.isNearClimbable() && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -SimpleCollisionBox.COLLISION_EPSILON, 0.5)))) {

            // Calculate the Y velocity after friction
            Vector hackyClimbVector = new Vector(0, 0.2, 0);
            PredictionEngineNormal.staticVectorEndOfTick(player, hackyClimbVector);

            pointThreePossibilities.add(new VectorData(hackyClimbVector, VectorData.VectorType.ZeroPointZeroThree));
        }

        // This is a secure method to add jumping vectors to this list
        addJumpsToPossibilities(player, pointThreePossibilities);
        addExplosionToPossibilities(player, pointThreePossibilities);

        if (player.packetStateData.tryingToRiptide) {
            Vector riptideAddition = Riptide.getRiptideVelocity(player);
            pointThreePossibilities.add(new VectorData(player.clientVelocity.clone().add(riptideAddition), new VectorData(new Vector(), VectorData.VectorType.ZeroPointZeroThree), VectorData.VectorType.Trident));
        }

        possibleVelocities.addAll(applyInputsToVelocityPossibilities(player, pointThreePossibilities, speed));
    }

    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> returnVectors = new ArrayList<>();
        loopVectors(player, possibleVectors, speed, returnVectors);
        return returnVectors;
    }

    public void addFluidPushingToStartingVectors(GrimPlayer player, Set<VectorData> data) {
        for (VectorData vectorData : data) {
            // Sneaking in water
            if (vectorData.isKnockback() && player.baseTickAddition.lengthSquared() != 0) {
                vectorData.vector = vectorData.vector.add(player.baseTickAddition);
            }
            // Water pushing movement is affected by initial velocity due to 0.003 eating pushing in the past
            if (vectorData.isKnockback() && player.baseTickWaterPushing.lengthSquared() != 0) {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                    Vector vec3 = player.baseTickWaterPushing.clone();
                    if (Math.abs(vectorData.vector.getX()) < 0.003 && Math.abs(vectorData.vector.getZ()) < 0.003 && player.baseTickWaterPushing.length() < 0.0045000000000000005D) {
                        vec3 = vec3.normalize().multiply(0.0045000000000000005);
                    }

                    vectorData.vector = vectorData.vector.add(vec3);
                } else {
                    vectorData.vector = vectorData.vector.add(player.baseTickWaterPushing);
                }
            }
        }
    }

    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        // Swim hop, riptide bounce, climbing, slime block bounces, knockback
        Set<VectorData> velocities = player.getPossibleVelocities();
        // Packet stuff is done first
        addExplosionToPossibilities(player, velocities);

        if (player.packetStateData.tryingToRiptide) {
            Vector riptideAddition = Riptide.getRiptideVelocity(player);
            velocities.add(new VectorData(player.clientVelocity.clone().add(riptideAddition), VectorData.VectorType.Trident));
        }

        // Fluid pushing is done BEFORE 0.003
        addFluidPushingToStartingVectors(player, velocities);
        // Inputs are done AFTER fluid pushing, https://github.com/MWHunter/Grim/issues/660
        addAttackSlowToPossibilities(player, velocities);
        // Non-effective AI for vehicles is done AFTER fluid pushing but BEFORE 0.003
        addNonEffectiveAI(player, velocities);
        // Attack slowing is done BEFORE 0.003! Moving this before 0.003 will cause falses!
        applyMovementThreshold(player, velocities);
        // Jumps are done after 0.003, for sure.
        addJumpsToPossibilities(player, velocities);

        return velocities;
    }

    private void addNonEffectiveAI(GrimPlayer player, Set<VectorData> data) {
        if (!player.compensatedEntities.getSelf().inVehicle()) return;

        for (VectorData vectorData : data) {
            vectorData.vector = vectorData.vector.clone().multiply(0.98);
        }
    }

    private void addAttackSlowToPossibilities(GrimPlayer player, Set<VectorData> velocities) {
        for (int x = 1; x <= Math.min(player.maxPlayerAttackSlow, 5); x++) {
            for (VectorData data : new HashSet<>(velocities)) {
                velocities.add(data.returnNewModified(data.vector.clone().multiply(new Vector(0.6, 1, 0.6)), VectorData.VectorType.AttackSlow));
            }
        }
    }

    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
    }

    // Renamed from applyPointZeroZeroThree to avoid confusion with applyZeroPointZeroThree
    public void applyMovementThreshold(GrimPlayer player, Set<VectorData> velocities) {
        double minimumMovement = 0.003D;
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            minimumMovement = 0.005D;
        }

        for (VectorData vector : velocities) {
            if (Math.abs(vector.vector.getX()) < minimumMovement) {
                vector.vector.setX(0D);
            }

            if (Math.abs(vector.vector.getY()) < minimumMovement) {
                vector.vector.setY(0D);
            }

            if (Math.abs(vector.vector.getZ()) < minimumMovement) {
                vector.vector.setZ(0D);
            }
        }
    }

    public void addExplosionToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.likelyExplosions != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.likelyExplosions.vector), vector, VectorData.VectorType.Explosion));
            }

            if (player.firstBreadExplosion != null) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(player.firstBreadExplosion.vector), vector, VectorData.VectorType.Explosion)
                        .returnNewModified(vector.vector.clone().add(player.firstBreadExplosion.vector), VectorData.VectorType.FirstBreadExplosion));
            }
        }
    }

    public int sortVectorData(VectorData a, VectorData b, GrimPlayer player) {
        int aScore = 0;
        int bScore = 0;

        // Order priority (to avoid false positives and false flagging future predictions):
        // Knockback and explosions
        // 0.03 ticks
        // Normal movement
        // First bread knockback and explosions
        // Flagging groundspoof
        // Flagging flip items
        if (a.isExplosion())
            aScore -= 5;

        if (a.isKnockback())
            aScore -= 5;

        if (b.isExplosion())
            bScore -= 5;

        if (b.isKnockback())
            bScore -= 5;

        if (a.isFirstBreadExplosion())
            aScore += 1;

        if (b.isFirstBreadExplosion())
            bScore += 1;

        if (a.isFirstBreadKb())
            aScore += 1;

        if (b.isFirstBreadKb())
            bScore += 1;

        if (a.isFlipItem())
            aScore += 3;

        if (b.isFlipItem())
            bScore += 3;

        if (a.isZeroPointZeroThree())
            aScore -= 1;

        if (b.isZeroPointZeroThree())
            bScore -= 1;

        // If the player is on the ground but the vector leads the player off the ground
        if ((player.compensatedEntities.getSelf().inVehicle() ? player.clientControlledVerticalCollision : player.onGround) && a.vector.getY() >= 0)
            aScore += 2;

        if ((player.compensatedEntities.getSelf().inVehicle() ? player.clientControlledVerticalCollision : player.onGround) && b.vector.getY() >= 0)
            bScore += 2;

        if (aScore != bScore)
            return Integer.compare(aScore, bScore);

        return Double.compare(a.vector.distanceSquared(player.actualMovement), b.vector.distanceSquared(player.actualMovement));
    }

    public Vector handleStartingVelocityUncertainty(GrimPlayer player, VectorData vector, Vector targetVec) {
        double avgColliding = Collections.max(player.uncertaintyHandler.collidingEntities);

        double additionHorizontal = player.uncertaintyHandler.getOffsetHorizontal(vector);
        double additionVertical = player.uncertaintyHandler.getVerticalOffset(vector);

        double pistonX = Collections.max(player.uncertaintyHandler.pistonX);
        double pistonY = Collections.max(player.uncertaintyHandler.pistonY);
        double pistonZ = Collections.max(player.uncertaintyHandler.pistonZ);

        additionHorizontal += player.uncertaintyHandler.lastHorizontalOffset;
        additionVertical += player.uncertaintyHandler.lastVerticalOffset;

        VectorData originalVec = vector;
        while (originalVec.lastVector != null) {
            originalVec = originalVec.lastVector;
        }

        // "temporary" workaround for when player toggles flight
        // Difficult as there are a ton of edge cases and version differences with flying
        // For example, try toggling not using elytra to flying without this hack
        double bonusY = 0;
        if (player.uncertaintyHandler.lastFlyingStatusChange.hasOccurredSince(4)) {
            additionHorizontal += 0.3;
            bonusY += 0.3;
        }

        if (player.uncertaintyHandler.lastUnderwaterFlyingHack.hasOccurredSince(9)) {
            bonusY += 0.2;
        }

        if (player.uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(2)) {
            additionHorizontal += 0.1;
            bonusY += 0.1;
        }

        if (pistonX != 0 || pistonY != 0 || pistonZ != 0) {
            additionHorizontal += 0.1;
            bonusY += 0.1;
        }

        // Handle horizontal fluid pushing within 0.03
        double horizontalFluid = player.pointThreeEstimator.getHorizontalFluidPushingUncertainty(vector);
        additionHorizontal += horizontalFluid;

        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        // 0.03 was falsing when colliding with https://i.imgur.com/7obfxG6.png
        // 0.065 was causing issues with fast moving dolphins
        // 0.075 seems safe?
        //
        // Be somewhat careful as there is an antikb (for horizontal) that relies on this lenience
        Vector uncertainty = new Vector(avgColliding * 0.08, additionVertical, avgColliding * 0.08);
        Vector min = new Vector(player.uncertaintyHandler.xNegativeUncertainty - additionHorizontal, -bonusY + player.uncertaintyHandler.yNegativeUncertainty, player.uncertaintyHandler.zNegativeUncertainty - additionHorizontal);
        Vector max = new Vector(player.uncertaintyHandler.xPositiveUncertainty + additionHorizontal, bonusY + player.uncertaintyHandler.yPositiveUncertainty, player.uncertaintyHandler.zPositiveUncertainty + additionHorizontal);

        Vector minVector = vector.vector.clone().add(min.subtract(uncertainty));
        Vector maxVector = vector.vector.clone().add(max.add(uncertainty));

        // Handle the player landing within 0.03 movement, which resets Y velocity
        if (player.uncertaintyHandler.onGroundUncertain && vector.vector.getY() < 0) {
            maxVector.setY(0);
        }

        // Handles stuff like missing idle packet causing gravity to be missed (plus 0.03 of course)
        double gravityOffset = player.pointThreeEstimator.getAdditionalVerticalUncertainty(vector);
        if (gravityOffset > 0) {
            maxVector.setY(maxVector.getY() + gravityOffset);
        } else {
            minVector.setY(minVector.getY() + gravityOffset);
        }

        // Handle vertical fluid pushing within 0.03
        double verticalFluid = player.pointThreeEstimator.getVerticalFluidPushingUncertainty(vector);
        minVector.setY(minVector.getY() - verticalFluid);

        // Handle vertical bubble column stupidity within 0.03
        double bubbleFluid = player.pointThreeEstimator.getVerticalBubbleUncertainty(vector);
        maxVector.setY(maxVector.getY() + bubbleFluid);
        minVector.setY(minVector.getY() - bubbleFluid);

        // We can't simulate the player's Y velocity, unknown number of ticks with a gravity change
        // Feel free to simulate all 104857600000000000000000000 possibilities!
        if (!player.pointThreeEstimator.canPredictNextVerticalMovement()) {
            minVector.setY(minVector.getY() - 0.08);
        }


        // Hidden slime block bounces by missing idle tick and 0.03
        if (player.actualMovement.getY() >= 0 && player.uncertaintyHandler.influencedByBouncyBlock()) {
            if (player.uncertaintyHandler.thisTickSlimeBlockUncertainty != 0 && !vector.isJump()) { // jumping overrides slime block
                if (player.uncertaintyHandler.thisTickSlimeBlockUncertainty > maxVector.getY()) {
                    maxVector.setY(player.uncertaintyHandler.thisTickSlimeBlockUncertainty);
                }
                if (minVector.getY() > 0) minVector.setY(0);
            }
        }

        if (vector.isZeroPointZeroThree() && vector.isSwimHop()) {
            minVector.setY(minVector.getY() - 0.06); // Fluid pushing downwards hidden by 0.03
        }

        SimpleCollisionBox box = new SimpleCollisionBox(minVector, maxVector);
        box.sort();

        // https://github.com/MWHunter/Grim/issues/398
        // Thank mojang for removing the idle packet resulting in this hacky mess

        double levitation = player.pointThreeEstimator.positiveLevitation(maxVector.getY());
        box.combineToMinimum(box.minX, levitation, box.minZ);
        levitation = player.pointThreeEstimator.positiveLevitation(minVector.getY());
        box.combineToMinimum(box.minX, levitation, box.minZ);
        levitation = player.pointThreeEstimator.negativeLevitation(maxVector.getY());
        box.combineToMinimum(box.minX, levitation, box.minZ);
        levitation = player.pointThreeEstimator.negativeLevitation(minVector.getY());
        box.combineToMinimum(box.minX, levitation, box.minZ);


        SneakingEstimator sneaking = player.checkManager.getPostPredictionCheck(SneakingEstimator.class);
        box.minX += sneaking.getSneakingPotentialHiddenVelocity().minX;
        box.minZ += sneaking.getSneakingPotentialHiddenVelocity().minZ;
        box.maxX += sneaking.getSneakingPotentialHiddenVelocity().maxX;
        box.maxZ += sneaking.getSneakingPotentialHiddenVelocity().maxZ;

        if (player.uncertaintyHandler.fireworksBox != null) {
            double minXdiff = Math.min(0, player.uncertaintyHandler.fireworksBox.minX - originalVec.vector.getX());
            double minYdiff = Math.min(0, player.uncertaintyHandler.fireworksBox.minY - originalVec.vector.getY());
            double minZdiff = Math.min(0, player.uncertaintyHandler.fireworksBox.minZ - originalVec.vector.getZ());
            double maxXdiff = Math.max(0, player.uncertaintyHandler.fireworksBox.maxX - originalVec.vector.getX());
            double maxYdiff = Math.max(0, player.uncertaintyHandler.fireworksBox.maxY - originalVec.vector.getY());
            double maxZdiff = Math.max(0, player.uncertaintyHandler.fireworksBox.maxZ - originalVec.vector.getZ());

            box.expandMin(minXdiff, minYdiff, minZdiff);
            box.expandMax(maxXdiff, maxYdiff, maxZdiff);
        }

        SimpleCollisionBox rod = player.uncertaintyHandler.fishingRodPullBox;
        if (rod != null) {
            box.expandMin(rod.minX, rod.minY, rod.minZ);
            box.expandMax(rod.maxX, rod.maxY, rod.maxZ);
        }

        // Player velocity can multiply 0.4-0.45 (guess on max) when the player is on slime with
        // a Y velocity of 0 to 0.1.  Because 0.03 we don't know this so just give lenience here
        //
        // Stuck on edge also reduces the player's movement.  It's wrong by 0.05 so hard to implement.
        if (player.uncertaintyHandler.stuckOnEdge.hasOccurredSince(0) || player.uncertaintyHandler.isSteppingOnSlime) {
            // Avoid changing Y axis
            box.expandToAbsoluteCoordinates(0, box.maxY, 0);
        }

        // Alright, so hard lerping entities are a pain to support.
        // A transaction splits with interpolation and suddenly your predictions are off by 20 blocks due to a collision not being seen
        // Or the player is on 1.9+ so you have no idea where the entity actually is.
        //
        // Or the player is on 1.9+ so you don't know how far the shulker has moved
        //
        //
        // Grim's old solution with hard lerping entities was to just give a ton of direct offset reduction
        // But that caused issues immediately after the uncertainty ended because then the player's calculated
        // clientVelocity was off because it was wrong because the offset reduction made the predictions "accurate"
        // but not the player's calculated velocity after friction.
        //
        // We also used to include the hard lerping entities into collisions, but not anymore.
        // It could be wrong and do the exact same thing, make the calculated offset wrong by a huge factor
        // and nothing can save it.
        //
        // The solution is that collisions are always less than the predicted movement
        // So by expanding to 0,0,0, the player can collide with absolutely any position
        // Yes, that allows a flight exploit, but not upwards which is important.
        // You can hover a block above a boat but who cares? The boat could easily just be a block upwards.
        //
        // Therefore, the friction movement for the next tick is correct.  Running it two ticks past the actual
        // hard lerping collision ensures that the friction remains correct (to the best in a sane amount of development effort)
        //
        // Also it's much faster not to look at every entity for every collision :) this hack saves compute time
        //
        // Or the player is on 1.14+ so you don't know how high their bounding box is making it so the player
        // jumps upwards and collides with a block, which you don't actually see because mojang removed the idle
        // packet and sneaking poses take 2 full ticks to apply
        //
        // Or the player is switching in and out of controlling a vehicle, in which friction messes it up
        //
        if (player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(0) || player.uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(3) || (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && vector.vector.getY() > 0 && vector.isZeroPointZeroThree() && !Collisions.isEmpty(player, GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, vector.vector.getY() + player.lastY + 0.6, player.lastZ, 0.6f, 1.26f)))) {
            box.expandToAbsoluteCoordinates(0, 0, 0);
        }

        // Handle missing a tick with friction in vehicles
        // TODO: Attempt to fix mojang's netcode here
        if (player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(1)) {
            double trueFriction = player.lastOnGround ? player.friction * 0.91 : 0.91;
            if (player.wasTouchingLava) trueFriction = 0.5;
            if (player.wasTouchingWater) trueFriction = 0.96;

            double maxY = Math.max(box.maxY, box.maxY + ((box.maxY - player.gravity) * 0.91));
            double minY = Math.min(box.minY, box.minY + ((box.minY - player.gravity) * 0.91));
            double minX = Math.min(box.minX, box.minX + (-player.speed * trueFriction));
            double minZ = Math.min(box.minZ, box.minZ + (-player.speed * trueFriction));
            double maxX = Math.max(box.maxX, box.maxX + (player.speed * trueFriction));
            double maxZ = Math.max(box.maxZ, box.maxZ + (player.speed * trueFriction));

            box = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
            box.expand(0.05, 0, 0.05); // Try value patching out any issues
        }

        if (player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(10)) {
            box.expand(0.001); // Ignore 1e-3 offsets as we don't know starting vel
        }

        minVector = box.min();
        maxVector = box.max();

        if (pistonX != 0) {
            minVector.setX(Math.min(minVector.getX() - pistonX, pistonX));
            maxVector.setX(Math.max(maxVector.getX() + pistonX, pistonX));
        }
        if (pistonY != 0) {
            minVector.setY(Math.min(minVector.getY() - pistonY, pistonY));
            maxVector.setY(Math.max(maxVector.getY() + pistonY, pistonY));
        }
        if (pistonZ != 0) {
            minVector.setZ(Math.min(minVector.getZ() - pistonZ, pistonZ));
            maxVector.setZ(Math.max(maxVector.getZ() + pistonZ, pistonZ));
        }
        return VectorUtils.cutBoxToVector(targetVec, minVector, maxVector);
    }

    public void endOfTick(GrimPlayer player, double d) {
        player.canSwimHop = canSwimHop(player);
        player.lastWasClimbing = 0;
    }

    private void loopVectors(GrimPlayer player, Set<VectorData> possibleVectors, float speed, List<VectorData> returnVectors) {
        // Stop omni-sprint
        // Optimization - Also cuts down scenarios by 2/3
        // For some reason the player sprints while swimming no matter what
        // Probably as a way to tell the server it is swimming
        int zMin = player.isSprinting && !player.isSwimming ? 1 : -1;

        for (int loopSlowed = 0; loopSlowed <= 1; loopSlowed++) {
            // Loop twice for the using item status if the player is using a trident
            // (Or in the future mojang desync's with another item and we can't be sure)
            //
            // I tried using delays, vertical collision detection, and other methods for sneaking
            // But nothing works as well as brute force
            for (int loopUsingItem = 0; loopUsingItem <= 1; loopUsingItem++) {
                for (VectorData possibleLastTickOutput : possibleVectors) {
                    // Only do this when there is tick skipping
                    if (loopSlowed == 1 && !possibleLastTickOutput.isZeroPointZeroThree()) continue;
                    for (int x = -1; x <= 1; x++) {
                        for (int z = zMin; z <= 1; z++) {
                            VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(getMovementResultFromInput(player, transformInputsToVector(player, new Vector(x, 0, z)), speed, player.xRot)), possibleLastTickOutput, VectorData.VectorType.InputResult);
                            result = result.returnNewModified(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
                            result = result.returnNewModified(handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
                            // Signal that we need to flip sneaking bounding box
                            if (loopUsingItem == 1)
                                result = result.returnNewModified(result.vector, VectorData.VectorType.Flip_Use_Item);
                            returnVectors.add(result);
                        }
                    }
                }

                player.packetStateData.slowedByUsingItem = !player.packetStateData.slowedByUsingItem;
            }
            // TODO: Secure this? Do we care about minor 1.9-1.18.1 (not 1.18.2+!) bypasses that no client exploits yet?
            // I personally don't care because 1.8 and 1.18.2 are much more popular than any weird version
            // Who would notice a tick of non-slow movement when netcode is so terrible that it just looks normal
            player.isSlowMovement = !player.isSlowMovement;
        }
    }

    public boolean canSwimHop(GrimPlayer player) {
        // Boats cannot swim hop, all other living entities should be able to.
        if (player.compensatedEntities.getSelf().getRiding() != null && EntityTypes.isTypeInstanceOf(player.compensatedEntities.getSelf().getRiding().type, EntityTypes.BOAT))
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
        //
        // Oh, also don't forget that the player can swim hop when colliding with boats (and shulkers)
        // We therefore check the hard lerping entity variable
        //
        // Don't play with poses issues. just assume full bounding box
        // Except on vehicles which don't have poses, thankfully.
        //
        SimpleCollisionBox oldBox = player.compensatedEntities.getSelf().inVehicle() ? GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ) :
                GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.6f, 1.8f);

        if (!player.compensatedWorld.containsLiquid(oldBox.expand(0.1, 0.1, 0.1))) return false;

        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = player.boundingBox.copy().expand(-0.03, 0, -0.03);
        // By flipping the distance to the ground, we can avoid players from swim hopping on the floor
        // Although it is unclear what advantage this would even give.
        double pointThreeToGround = Collisions.collide(player, 0, -0.03, 0).getY() + SimpleCollisionBox.COLLISION_EPSILON;
        player.boundingBox = oldBB;

        SimpleCollisionBox newBox = player.compensatedEntities.getSelf().inVehicle() ? GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z) :
                GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 1.8f);

        return player.uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(3) || !Collisions.isEmpty(player, newBox.expand(player.clientVelocity.getX(), -1 * pointThreeToGround, player.clientVelocity.getZ()).expand(0.5, 0.03, 0.5));
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

    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        return vector;
    }

    public void doJump(GrimPlayer player, Vector vector) {
        if (!player.lastOnGround || player.onGround)
            return;

        JumpPower.jumpFromGround(player, vector);
    }
}
