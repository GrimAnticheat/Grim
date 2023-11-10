package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PredictionEngineRideableUtils {
    public static Set<VectorData> handleJumps(GrimPlayer player, Set<VectorData> possibleVectors) {
        if (!(player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityHorse)) return possibleVectors;

        PacketEntityHorse horse = (PacketEntityHorse) player.compensatedEntities.getSelf().getRiding();

        // Setup player inputs
        // f is not used
        // float f = player.vehicleData.vehicleHorizontal * 0.5F;
        float f1 = player.vehicleData.vehicleForward;

        if (f1 <= 0.0F) {
            f1 *= 0.25F;
        }

        double d1;
        double d0;

        double movementSpeed = horse.movementSpeedAttribute;
        double jumpStrength = horse.jumpStrength;

        // If the player wants to jump on a horse
        // Listen to Entity Action -> start jump with horse, stop jump with horse
        //
        // There's a float/double error causing 1e-8 imprecision if anyone wants to debug it
        if (horse.type == EntityTypes.CAMEL) {
            if (player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround) {
                horse.isDashing = true;
                d0 = jumpStrength * (double) JumpPower.getPlayerJumpFactor(player);

                if (player.compensatedEntities.getJumpAmplifier() != null) {
                    d1 = d0 + ((player.compensatedEntities.getJumpAmplifier() + 1) * 0.1F);
                } else {
                    d1 = d0;
                }

                double multiplier = (double) (22.2222F * player.vehicleData.horseJump) * (movementSpeed) * (double) BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));
                Vector vec = ReachUtils.getLook(player, player.xRot, player.yRot).multiply(new Vector(1.0, 0.0, 1.0)).normalize().multiply(multiplier).add(new Vector(0, (double) (1.4285F * player.vehicleData.horseJump) * d1, 0));

                for (VectorData vectorData : possibleVectors) {
                    vectorData.vector.add(vec);
                }

                player.vehicleData.dashCooldown = 55;
            }
        } else {
            if (player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround) {
                d0 = jumpStrength * player.vehicleData.horseJump * JumpPower.getPlayerJumpFactor(player);

                // This doesn't even work because vehicle jump boost has (likely) been
                // broken ever since vehicle control became client sided
                //
                // But plugins can still send this, so support it anyways
                if (player.compensatedEntities.getJumpAmplifier() != null) {
                    d1 = d0 + ((player.compensatedEntities.getJumpAmplifier() + 1) * 0.1F);
                } else {
                    d1 = d0;
                }



                float f2 = player.trigHandler.sin(player.xRot * ((float) Math.PI / 180F));
                float f3 = player.trigHandler.cos(player.xRot * ((float) Math.PI / 180F));

                for (VectorData vectorData : possibleVectors) {
                    vectorData.vector.setY(d1);
                    if (f1 > 0.0F) {
                        vectorData.vector.add(new Vector(-0.4F * f2 * player.vehicleData.horseJump, 0.0D, 0.4F * f3 * player.vehicleData.horseJump));
                    }
                }

            }

        }

        player.vehicleData.horseJumping = true;
        player.vehicleData.horseJump = 0.0F;

        // More jumping stuff
        if (player.lastOnGround) {
            player.vehicleData.horseJumping = false;
        }

        return possibleVectors;
    }

    public static List<VectorData> applyInputsToVelocityPossibilities(Vector movementVector, GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> returnVectors = new ArrayList<>();

        for (VectorData possibleLastTickOutput : possibleVectors) {
            VectorData result = new VectorData(possibleLastTickOutput.vector.clone().add(new PredictionEngine().getMovementResultFromInput(player, movementVector, speed, player.xRot)), possibleLastTickOutput, VectorData.VectorType.InputResult);
            result = result.returnNewModified(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
            result = result.returnNewModified(new PredictionEngineNormal().handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
            returnVectors.add(result);

            // This is the laziest way to reduce false positives such as horse rearing
            // No bypasses can ever be derived from this, so why not?
            result = new VectorData(possibleLastTickOutput.vector.clone(), possibleLastTickOutput, VectorData.VectorType.InputResult);
            result = result.returnNewModified(result.vector.clone().multiply(player.stuckSpeedMultiplier), VectorData.VectorType.StuckMultiplier);
            result = result.returnNewModified(new PredictionEngineNormal().handleOnClimbable(result.vector.clone(), player), VectorData.VectorType.Climbable);
            returnVectors.add(result);
        }

        return returnVectors;
    }
}