package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {
    public static final Material scaffolding = XMaterial.SCAFFOLDING.parseMaterial();

    public static void staticVectorEndOfTick(GrimPlayer player, Vector vector) {
        double d9 = vector.getY();
        if (player.levitationAmplifier > 0) {
            d9 += (0.05 * (double) (player.levitationAmplifier + 1) - vector.getY()) * 0.2;
        } else if (player.compensatedWorld.getChunk((int) player.x >> 4, (int) player.z >> 4) != null) {
            // Commenting out hasGravity check because players always have gravity
            d9 -= player.gravity;
        } else {
            d9 = vector.getY() > 0.0 ? -0.1 : 0.0;
        }

        vector.setX(vector.getX() * player.friction);
        vector.setY(d9 * 0.9800000190734863);
        vector.setZ(vector.getZ() * player.friction);
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            Vector jump = vector.vector.clone();

            if (!player.specialFlying) {
                // If the player didn't try to jump
                // And 0.03 didn't affect onGround status
                // The player cannot jump
                if ((!player.lastOnGround || player.onGround) && !(player.uncertaintyHandler.lastPacketWasGroundPacket && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree))
                    return;

                JumpPower.jumpFromGround(player, jump);
            } else {
                jump.add(new Vector(0, player.flySpeed * 3, 0));
            }

            existingVelocities.add(new VectorData(jump, VectorData.VectorType.Jump));
        }
    }

    @Override
    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        if (player.lastClimbing) {
            vector.setX(GrimMathHelper.clamp(vector.getX(), -0.15F, 0.15F));
            vector.setZ(GrimMathHelper.clamp(vector.getZ(), -0.15F, 0.15F));
            vector.setY(Math.max(vector.getY(), -0.15F));

            // Yes, this uses shifting not crouching
            if (vector.getY() < 0.0 && !(player.compensatedWorld.getBukkitMaterialAt(player.lastX, player.lastY, player.lastZ) == scaffolding) && player.isSneaking && !player.specialFlying) {
                vector.setY(0.0);
            }
        }

        return vector;
    }

    @Override
    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        Set<VectorData> regularInputs = super.fetchPossibleStartTickVectors(player);

        // This is WRONG! Vanilla has this system at the end
        // However, due to 1.9 reduced movement precision, we aren't informed that the player could have this velocity
        // We still do climbing at the end, as it uses a different client velocity
        //
        // Force 1.13.2 and below players to have something to collide with horizontally to climb
        if (player.isClimbing && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -0.01, 0.5)))) {

            // Calculate the Y velocity after friction
            Vector hackyClimbVector = player.clientVelocity.clone().setY(0.2);
            staticVectorEndOfTick(player, hackyClimbVector);
            hackyClimbVector.setX(player.clientVelocity.getX());
            hackyClimbVector.setZ(player.clientVelocity.getZ());

            regularInputs.add(new VectorData(hackyClimbVector, VectorData.VectorType.HackyClimbable));
        }

        return regularInputs;
    }

    @Override
    public void endOfTick(GrimPlayer player, double d, float friction) {
        player.clientVelocityOnLadder = null;

        // Force 1.13.2 and below players to have something to collide with horizontally to climb-
        if (player.isClimbing && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -0.01, 0.5)))) {
            player.clientVelocityOnLadder = player.clientVelocity.clone().setY(0.2);
        }

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(player, vector.vector);
        }

        super.endOfTick(player, d, friction);
    }
}
