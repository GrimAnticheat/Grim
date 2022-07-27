package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PredictionEngineElytra extends PredictionEngine {
    // Inputs have no effect on movement
    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> results = new ArrayList<>();
        Vector currentLook = ReachUtils.getLook(player, player.xRot, player.yRot);

        for (VectorData data : possibleVectors) {
            Vector elytraResult = getElytraMovement(player, data.vector.clone(), currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99F, 0.98F, 0.99F));
            results.add(data.returnNewModified(elytraResult, VectorData.VectorType.InputResult));

            // We must bruteforce Optifine ShitMath
            player.trigHandler.toggleShitMath();
            elytraResult = getElytraMovement(player, data.vector.clone(), ReachUtils.getLook(player, player.xRot, player.yRot)).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99F, 0.98F, 0.99F));
            player.trigHandler.toggleShitMath();
            results.add(data.returnNewModified(elytraResult, VectorData.VectorType.InputResult));
        }

        return results;
    }

    public static Vector getElytraMovement(GrimPlayer player, Vector vector, Vector lookVector) {
        float yRotRadians = player.yRot * 0.017453292F;
        double horizontalSqrt = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double horizontalLength = vector.clone().setY(0).length();
        double length = lookVector.length();
        // Mojang changed from using their math to using regular java math in 1.18.2 elytra movement
        double vertCosRotation = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2) ? Math.cos(yRotRadians) : player.trigHandler.cos(yRotRadians);
        vertCosRotation = (float) (vertCosRotation * vertCosRotation * Math.min(1.0D, length / 0.4D));
        // So we actually use the player's actual movement to get the gravity/slow falling status
        // However, this is wrong with elytra movement because players can control vertical movement after gravity is calculated
        // Yeah, slow falling needs a refactor in grim.
        double recalculatedGravity = 0.08;
        if (player.clientVelocity.getY() <= 0 && player.compensatedEntities.getSlowFallingAmplifier() != null)
            recalculatedGravity = 0.01;

        vector.add(new Vector(0.0D, recalculatedGravity * (-1.0D + vertCosRotation * 0.75D), 0.0D));
        double d5;

        // Handle slowing the player down when falling
        if (vector.getY() < 0.0D && horizontalSqrt > 0.0D) {
            d5 = vector.getY() * -0.1D * vertCosRotation;
            vector.add(new Vector(lookVector.getX() * d5 / horizontalSqrt, d5, lookVector.getZ() * d5 / horizontalSqrt));
        }

        // Handle accelerating the player when they are looking down
        if (yRotRadians < 0.0F && horizontalSqrt > 0.0D) {
            d5 = horizontalLength * (double) (-player.trigHandler.sin(yRotRadians)) * 0.04D;
            vector.add(new Vector(-lookVector.getX() * d5 / horizontalSqrt, d5 * 3.2D, -lookVector.getZ() * d5 / horizontalSqrt));
        }

        // Handle accelerating the player sideways
        if (horizontalSqrt > 0) {
            vector.add(new Vector((lookVector.getX() / horizontalSqrt * horizontalLength - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / horizontalSqrt * horizontalLength - vector.getZ()) * 0.1D));
        }

        return vector;
    }

    // Yes... you can jump while using an elytra as long as you are on the ground
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        new PredictionEngineNormal().addJumpsToPossibilities(player, existingVelocities);
    }
}
