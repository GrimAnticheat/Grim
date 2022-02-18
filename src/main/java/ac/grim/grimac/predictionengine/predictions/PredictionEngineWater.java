package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.FluidFallingAdjustedMovement;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWater extends PredictionEngine {
    boolean isFalling;
    double playerGravity;
    float swimmingSpeed;
    float swimmingFriction;
    double lastY;

    public static void staticVectorEndOfTick(GrimPlayer player, Vector vector, float swimmingFriction, double playerGravity, boolean isFalling) {
        vector.multiply(new Vector(swimmingFriction, 0.8F, swimmingFriction));
        Vector fluidVector = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, vector);
        vector.setX(fluidVector.getX());
        vector.setY(fluidVector.getY());
        vector.setZ(fluidVector.getZ());
    }

    public static Set<VectorData> transformSwimmingVectors(GrimPlayer player, Set<VectorData> base) {
        Set<VectorData> swimmingVelocities = new HashSet<>();

        // Vanilla checks for swimming
        // We check for: eye in water (last tick for some versions)
        // fluid on eyes (current tick)
        // Or vanilla is swimming
        // Or last tick swimming
        //
        // This stops players from abusing this mechanic while on top of water, which could theoretically allow
        // some form of a new Jesus hack.
        // Anyways, Jesus doesn't make too much sense on 1.13+ clients anyways when swimming is faster
        if ((player.wasEyeInWater || player.fluidOnEyes == FluidTag.WATER || player.isSwimming || player.wasSwimming) && player.playerVehicle == null) {
            for (VectorData vector : base) {
                double d = getLookAngle(player).getY();
                double d5 = d < -0.2 ? 0.085 : 0.06;

                // The player can always press jump and activate this
                swimmingVelocities.add(vector.returnNewModified(new Vector(vector.vector.getX(), vector.vector.getY() + ((d - vector.vector.getY()) * d5), vector.vector.getZ()), VectorData.VectorType.SwimmingSpace));

                // This scenario will occur if the player does not press jump and the other conditions are met
                // Theoretically we should check this BEFORE allowing no look, but there isn't a cheat that takes advantage of this yet
                // The cheat would allow the player to move LESS than they would otherwise... which... why would you want to do that?
                // Anyways, netcode here with swimming is bad, so, just allow this unfair disadvantage that doesn't exist
                // If you feel adventurous, re-add the following line to eliminate this unfair disadvantage

                //if (d > 0.0 && player.compensatedWorld.getFluidLevelAt(player.lastX, player.lastY + 1.0 - 0.1, player.lastZ) == 0) {
                swimmingVelocities.add(vector.returnNewModified(vector.vector, VectorData.VectorType.SurfaceSwimming));

            }
            return swimmingVelocities;
        }
        return base;
    }

    public static Vector getLookAngle(GrimPlayer player) {
        return calculateViewVector(player, player.yRot, player.xRot);
    }

    public static Vector calculateViewVector(GrimPlayer player, float f, float f2) {
        float f3 = f * 0.017453292f;
        float f4 = -f2 * 0.017453292f;
        float f5 = player.trigHandler.cos(f4);
        float f6 = player.trigHandler.sin(f4);
        float f7 = player.trigHandler.cos(f3);
        float f8 = player.trigHandler.sin(f3);
        return new Vector(f6 * f7, -f8, f5 * f7);
    }

    public void guessBestMovement(float swimmingSpeed, GrimPlayer player, boolean isFalling, double playerGravity, float swimmingFriction, double lastY) {
        this.isFalling = isFalling;
        this.playerGravity = playerGravity;
        this.swimmingSpeed = swimmingSpeed;
        this.swimmingFriction = swimmingFriction;
        this.lastY = lastY;
        super.guessBestMovement(swimmingSpeed, player);
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(vector.returnNewModified(vector.vector.clone().add(new Vector(0, 0.04f, 0)), VectorData.VectorType.Jump));

            if (player.slightlyTouchingWater && player.lastOnGround && !player.onGround) {
                Vector withJump = vector.vector.clone();
                super.doJump(player, withJump);
                existingVelocities.add(new VectorData(withJump, vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double playerGravity, float friction) {
        super.endOfTick(player, playerGravity, friction);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            staticVectorEndOfTick(player, vector.vector, swimmingFriction, playerGravity, isFalling);
        }
    }

    @Override
    public Set<VectorData> fetchPossibleStartTickVectors(GrimPlayer player) {
        // "hacky" climbing where player enters ladder within 0.03 movement (WHY DOES 0.03 EXIST???)
        if (player.lastWasClimbing == 0 && player.pointThreeEstimator.isNearClimbable() && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) || !Collisions.isEmpty(player, player.boundingBox.copy().expand(
                player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0.5, -SimpleCollisionBox.COLLISION_EPSILON, 0.5)))) {
            player.lastWasClimbing = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.2D * 0.8F)).getY();
        }

        Set<VectorData> baseVelocities = super.fetchPossibleStartTickVectors(player);

        return transformSwimmingVectors(player, baseVelocities);
    }
}
