package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.JumpPower;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {
    private static final Material SCAFFOLDING = XMaterial.SCAFFOLDING.parseMaterial();
    private static final Material POWDER_SNOW = XMaterial.POWDER_SNOW.parseMaterial();

    private static final Material LEATHER_BOOTS = XMaterial.LEATHER_BOOTS.parseMaterial();

    public static void staticVectorEndOfTick(GrimPlayer player, Vector vector) {
        double d9 = vector.getY();
        if (player.levitationAmplifier > 0) {
            d9 += (0.05 * (double) (player.levitationAmplifier) - vector.getY()) * 0.2;
            // Reset fall distance with levitation
            player.fallDistance = 0;
        } else if (player.compensatedWorld.getChunk((int) player.x >> 4, (int) player.z >> 4) != null) {
            // Commenting out hasGravity check because players always have gravity
            d9 -= player.gravity;
        } else {
            d9 = vector.getY() > 0.0 ? -0.1 : 0.0;
        }

        vector.setX(vector.getX() * (double) player.friction);
        vector.setY(d9 * (double) 0.98F);
        vector.setZ(vector.getZ() * (double) player.friction);
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
                if (!player.wasFlying) {
                    Vector edgeCaseJump = jump.clone();
                    JumpPower.jumpFromGround(player, edgeCaseJump);
                    existingVelocities.add(vector.returnNewModified(edgeCaseJump, VectorData.VectorType.Jump));
                }
            }

            existingVelocities.add(vector.returnNewModified(jump, VectorData.VectorType.Jump));
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double d, float friction) {
        super.endOfTick(player, d, friction);

        boolean walkingOnPowderSnow = false;

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17) &&
                player.compensatedWorld.getBukkitMaterialAt(player.x, player.y, player.z) == POWDER_SNOW) {
            ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
            walkingOnPowderSnow = boots != null && boots.getType() == LEATHER_BOOTS;
        }

        // Force 1.13.2 and below players to have something to collide with horizontally to climb
        if (player.isClimbing && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -0.01, 0.5))) || walkingOnPowderSnow) {
            Vector ladder = player.clientVelocity.clone().setY(0.2);
            staticVectorEndOfTick(player, ladder);
            player.lastWasClimbing = ladder.getY();
        }

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(player, vector.vector);
        }
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
    public Vector handleOnClimbable(Vector vector, GrimPlayer player) {
        if (player.lastClimbing) {
            // Reset fall distance when climbing
            player.fallDistance = 0;

            vector.setX(GrimMathHelper.clamp(vector.getX(), -0.15F, 0.15F));
            vector.setZ(GrimMathHelper.clamp(vector.getZ(), -0.15F, 0.15F));
            vector.setY(Math.max(vector.getY(), -0.15F));

            // Yes, this uses shifting not crouching
            if (vector.getY() < 0.0 && !(player.compensatedWorld.getBukkitMaterialAt(player.lastX, player.lastY, player.lastZ) == SCAFFOLDING) && player.isSneaking && !player.specialFlying) {
                vector.setY(0.0);
            }
        }

        return vector;
    }
}
