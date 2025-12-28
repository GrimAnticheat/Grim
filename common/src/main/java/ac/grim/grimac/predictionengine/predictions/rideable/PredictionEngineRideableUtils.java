package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityCamel;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

@UtilityClass
public final class PredictionEngineRideableUtils {

    public static Set<VectorData> handleJumps(GrimPlayer player, Set<VectorData> possibleVectors) {
        if (!(player.compensatedEntities.self.getRiding() instanceof PacketEntityHorse horse))
            return possibleVectors;

        if (horse instanceof PacketEntityCamel camel) {
            handleCamelDash(player, possibleVectors, camel);
        } else {
            handleHorseJumping(player, possibleVectors, horse);
        }

        // More jumping stuff
        if (player.lastOnGround) {
            player.vehicleData.horseJump = 0.0F;
            player.vehicleData.horseJumping = false;
        }

        return possibleVectors;
    }

    private static void handleCamelDash(GrimPlayer player, Set<VectorData> possibleVectors, PacketEntityCamel camel) {
        final boolean wantsToJump = player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround;
        if (!wantsToJump) return;

        final double jumpFactor = camel.getAttributeValue(Attributes.JUMP_STRENGTH) * JumpPower.getPlayerJumpFactor(player);

        double jumpYVelocity;

        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (jumpBoost.isPresent()) {
            jumpYVelocity = jumpFactor + ((jumpBoost.getAsInt() + 1) * 0.1F);
        } else {
            jumpYVelocity = jumpFactor;
        }

        final double multiplier = (double) (22.2222F * player.vehicleData.horseJump) * camel.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double) BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
        Vector3dm jumpVelocity = ReachUtils.getLook(player, player.yaw, player.pitch).multiply(1.0, 0.0, 1.0).normalize().multiply(multiplier).add(0, (double) (1.4285F * player.vehicleData.horseJump) * jumpYVelocity, 0);

        for (VectorData vectorData : possibleVectors) {
            vectorData.vector.add(jumpVelocity);
        }

        player.vehicleData.horseJumping = true;
        player.vehicleData.camelDashCooldown = 55;
    }

    private static void handleHorseJumping(GrimPlayer player, Set<VectorData> possibleVectors, PacketEntityHorse horse) {
        // If the player wants to jump on a horse
        // Listen to Entity Action -> start jump with horse, stop jump with horse
        final boolean wantsToJump = player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround;
        if (!wantsToJump) return;

        float forwardInput = player.vehicleData.vehicleForward;

        if (forwardInput <= 0.0F) {
            forwardInput *= 0.25F;
        }

        double jumpFactor = (float) horse.getAttributeValue(Attributes.JUMP_STRENGTH) * player.vehicleData.horseJump * JumpPower.getPlayerJumpFactor(player);
        double jumpVelocity;

        // This doesn't even work because vehicle jump boost has (likely) been
        // broken ever since vehicle control became client sided
        //
        // But plugins can still send this, so support it anyways
        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (jumpBoost.isPresent()) {
            jumpVelocity = jumpFactor + ((jumpBoost.getAsInt() + 1) * 0.1F);
        } else {
            jumpVelocity = jumpFactor;
        }

        player.vehicleData.horseJumping = true;

        float yawRadians = GrimMath.radians(player.yaw);
        float f2 = player.trigHandler.sin(yawRadians);
        float f3 = player.trigHandler.cos(yawRadians);

        for (VectorData vectorData : possibleVectors) {
            vectorData.vector.setY(jumpVelocity);
            if (forwardInput > 0.0F) {
                vectorData.vector.add(new Vector3dm(-0.4F * f2 * player.vehicleData.horseJump, 0.0D, 0.4F * f3 * player.vehicleData.horseJump));
            }
        }

        player.vehicleData.horseJump = 0.0F;
    }

    public static List<VectorData> applyInputsToVelocityPossibilities(Vector3dm movementVector, GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        return applyInputsToVelocityPossibilities(new PredictionEngine(), movementVector, player, possibleVectors, speed);
    }

    public static List<VectorData> applyInputsToVelocityPossibilities(PredictionEngine predictionEngine, Vector3dm movementVector, GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> returnVectors = new ArrayList<>();

        for (VectorData possibleLastTickOutput : possibleVectors) {
            for (int applyStuckSpeed = 1; applyStuckSpeed >= 0; applyStuckSpeed--) {
                if (applyStuckSpeed == 0 && player.isForceStuckSpeed()) break;

                VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(predictionEngine.getMovementResultFromInput(player, movementVector, speed, player.yaw)), possibleLastTickOutput, VectorData.VectorType.InputResult);
                result.input = new Vector3dm(player.vehicleData.vehicleForward, 0, player.vehicleData.vehicleHorizontal);
                Vector3dm vector = result.vector.clone();
                if (applyStuckSpeed != 0) vector.multiply(player.stuckSpeedMultiplier);
                result = result.returnNewModified(vector, VectorData.VectorType.StuckMultiplier);
                result = result.returnNewModified(new PredictionEngineNormal().handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
                returnVectors.add(result);

                // This is the laziest way to reduce false positives such as horse rearing
                // No bypasses can ever be derived from this, so why not?
                result = new VectorData(possibleLastTickOutput.vector.clone(), possibleLastTickOutput, VectorData.VectorType.InputResult);
                result.input = new Vector3dm(player.vehicleData.vehicleForward, 0, player.vehicleData.vehicleHorizontal);
                vector = result.vector.clone();
                if (applyStuckSpeed != 0) vector.multiply(player.stuckSpeedMultiplier);
                result = result.returnNewModified(vector, VectorData.VectorType.StuckMultiplier);
                result = result.returnNewModified(new PredictionEngineNormal().handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
                returnVectors.add(result);
            }
        }

        return returnVectors;
    }
}
