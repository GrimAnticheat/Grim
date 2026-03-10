package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PredictionEngineElytra extends PredictionEngine {
    public static Vector3dm getElytraMovement(GrimPlayer player, Vector3dm vector, Vector3dm lookVector) {
        float pitchRadians = GrimMath.radians(player.pitch);
        double horizontalSqrt = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double horizontalLength = vector.clone().setY(0).length();
        double length = lookVector.length();

        // Mojang changed from using their math to using regular java math in 1.18.2 elytra movement
        double vertCosRotation = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2) ? Math.cos(pitchRadians) : player.trigHandler.cos(pitchRadians);
        vertCosRotation = (float) (vertCosRotation * vertCosRotation * Math.min(1.0D, length / 0.4D));

        // So we actually use the player's actual movement to get the gravity/slow falling status
        // However, this is wrong with elytra movement because players can control vertical movement after gravity is calculated
        // Yeah, slow falling needs a refactor in grim.
        double recalculatedGravity = player.compensatedEntities.self.getAttributeValue(Attributes.GRAVITY);
        if (player.clientVelocity.getY() <= 0 && player.compensatedEntities.getSlowFallingAmplifier().isPresent()) {
            recalculatedGravity = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5) ? 0.01 : Math.min(recalculatedGravity, 0.01);
        }

        vector.add(0.0D, recalculatedGravity * (-1.0D + vertCosRotation * 0.75D), 0.0D);
        double d5;

        // Handle slowing the player down when falling
        if (vector.getY() < 0.0D && horizontalSqrt > 0.0D) {
            d5 = vector.getY() * -0.1D * vertCosRotation;
            vector.add(lookVector.getX() * d5 / horizontalSqrt, d5, lookVector.getZ() * d5 / horizontalSqrt);
        }

        // Handle accelerating the player when they are looking down
        if (pitchRadians < 0.0F && horizontalSqrt > 0.0D) {
            d5 = horizontalLength * (double) (-player.trigHandler.sin(pitchRadians)) * 0.04D;
            vector.add(-lookVector.getX() * d5 / horizontalSqrt, d5 * 3.2D, -lookVector.getZ() * d5 / horizontalSqrt);
        }

        // Handle accelerating the player sideways
        if (horizontalSqrt > 0) {
            vector.add((lookVector.getX() / horizontalSqrt * horizontalLength - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / horizontalSqrt * horizontalLength - vector.getZ()) * 0.1D);
        }

        return vector;
    }

    // Inputs have no effect on movement
    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        List<VectorData> results = new ArrayList<>();

        // We must bruteforce Optifine ShitMath
        for (int shitmath = 0; shitmath <= 1; shitmath++, player.trigHandler.toggleShitMath()) {
            Vector3dm currentLook = ReachUtils.getLook(player, player.yaw, player.pitch);
            for (int applyStuckSpeed = 1; applyStuckSpeed >= 0; applyStuckSpeed--) {
                if (applyStuckSpeed == 0 && player.isForceStuckSpeed()) break;
                for (VectorData data : possibleVectors) {
                    Vector3dm elytraResult = getElytraMovement(player, data.vector.clone(), currentLook);
                    if (applyStuckSpeed != 0) elytraResult.multiply(player.stuckSpeedMultiplier);
                    elytraResult.multiply(0.99F, 0.98F, 0.99F);
                    VectorData modified = data.returnNewModified(elytraResult, VectorData.VectorType.InputResult);
                    modified.input = new Vector3dm(0, 0, 0);
                    results.add(modified);
                }
            }
        }

        return results;
    }

    // Yes... you can jump while using an elytra as long as you are on the ground
    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        new PredictionEngineNormal().addJumpsToPossibilities(player, existingVelocities);
    }
}
