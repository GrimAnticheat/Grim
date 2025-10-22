package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {

    public static double staticVectorEndOfTickY(GrimPlayer player, double y) {
        double adjustedY = y;
        final OptionalInt levitation = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.LEVITATION);
        if (levitation.isPresent()) {
            adjustedY += (0.05 * (levitation.getAsInt() + 1) - y) * 0.2;
            // Reset fall distance with levitation
            player.fallDistance = 0;
        } else if (player.hasGravity) {
            adjustedY -= player.gravity;
        }

        return adjustedY * 0.98F;
    }

    public static void staticVectorEndOfTick(GrimPlayer player, VectorData vectorData) {
        double adjustedY = vectorData.vectorY;
        final OptionalInt levitation = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.LEVITATION);
        if (levitation.isPresent()) {
            adjustedY += (0.05 * (levitation.getAsInt() + 1) - vectorData.vectorY) * 0.2;
            // Reset fall distance with levitation
            player.fallDistance = 0;
        } else if (player.hasGravity) {
            adjustedY -= player.gravity;
        }

        vectorData.vectorX = vectorData.vectorX * player.friction;
        vectorData.vectorY = adjustedY * 0.98F;
        vectorData.vectorZ = vectorData.vectorZ * player.friction;
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        if (player.supportsEndTick() && !player.packetStateData.knownInput.jump()) {
            return;
        }

        final VectorData[] vectorsToProcess = existingVelocities.toArray(new VectorData[0]);
        for (VectorData vector : vectorsToProcess) {
            double[] jumpValues = {vector.vectorX, vector.vectorY, vector.vectorZ};

            if (!player.isFlying) {
                // Negative jump boost does not allow the player to leave the ground
                // Negative jump boost doesn't seem to work in water/lava
                // If the player didn't try to jump
                // And 0.03 didn't affect onGround status
                // The player cannot jump
                final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
                if (((jumpBoost.isEmpty() || jumpBoost.getAsInt() >= 0) && player.onGround) || !player.lastOnGround)
                    return;

                JumpPower.jumpFromGround(player, jumpValues);
            } else {
                jumpValues[1] = player.flySpeed * 3;
                if (!player.wasFlying) {
                    double[] edgeCaseJumpValues = {jumpValues[0], jumpValues[1], jumpValues[2]};
                    JumpPower.jumpFromGround(player, edgeCaseJumpValues);
                    existingVelocities.add(vector.returnNewModified(edgeCaseJumpValues[0], edgeCaseJumpValues[1], edgeCaseJumpValues[2], VectorData.VectorType.Jump));
                }
            }

            existingVelocities.add(vector.returnNewModified(jumpValues[0], jumpValues[1], jumpValues[2], VectorData.VectorType.Jump));
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double delta) {
        super.endOfTick(player, delta);

        boolean walkingOnPowderSnow = false;

        if (!player.inVehicle() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) &&
                player.compensatedWorld.getBlockType(player.x, player.y, player.z) == StateTypes.POWDER_SNOW) {
            ItemStack boots = player.inventory.getBoots();
            walkingOnPowderSnow = boots != null && boots.getType() == ItemTypes.LEATHER_BOOTS;
        }

        player.isClimbing = Collisions.onClimbable(player, player.x, player.y, player.z);

        // Force 1.13.2 and below players to have something to collide with horizontally to climb
        if (player.lastWasClimbing == 0 && (player.pointThreeEstimator.isNearClimbable() || player.isClimbing) && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)
                || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -SimpleCollisionBox.COLLISION_EPSILON, 0.5))) || walkingOnPowderSnow) {
            player.lastWasClimbing = staticVectorEndOfTickY(player, 0.2);
        }

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(player, vector);
        }
    }

    @Override
    public Vector3dm handleOnClimbable(double x, double y, double z, GrimPlayer player) {
        if (player.isClimbing) {
            // Reset fall distance when climbing
            player.fallDistance = 0;

            x = GrimMath.clamp(x, -0.15F, 0.15F);
            z = GrimMath.clamp(z, -0.15F, 0.15F);
            y = Math.max(y, -0.15F);

            // Yes, this uses shifting not crouching
            if (y < 0.0 && !(player.compensatedWorld.getBlockType(player.lastX, player.lastY, player.lastZ) == StateTypes.SCAFFOLDING) && player.isSneaking && !player.isFlying) {
                y = 0.0;
            }
        }

        return new Vector3dm(x, y, z);
    }
}
