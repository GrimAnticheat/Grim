package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineNautilusWater;
import ac.grim.grimac.utils.data.packetentity.PacketEntityNautilus;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerNautilus extends MovementTickerLivingVehicle {

    public MovementTickerNautilus(GrimPlayer player) {
        super(player);

        PacketEntityNautilus nautilus = (PacketEntityNautilus) player.compensatedEntities.self.getRiding();
        if (!nautilus.hasSaddle()) return;

        player.speed = getRiddenSpeed(player);

        // Setup player inputs
        float sideways = player.vehicleData.vehicleHorizontal;
        float forward = 0.0F;
        float upAndDown = 0.0F;
        if (player.vehicleData.vehicleForward != 0.0F) {
            float xRot = player.pitch * 2F;
            float calcForward = player.trigHandler.cos(xRot * (float) (Math.PI / 180.0));
            float calcUpAndDown = -player.trigHandler.sin(xRot * (float) (Math.PI / 180.0));
            if (player.vehicleData.vehicleForward < 0.0F) {
                calcForward *= -0.5F;
                calcUpAndDown *= -0.5F;
            }

            upAndDown = calcUpAndDown;
            forward = calcForward;
        }

        this.movementInput = new Vector3dm(sideways, upAndDown, forward);
        if (this.movementInput.lengthSquared() > 1) this.movementInput.normalize();
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        new PredictionEngineNautilusWater(this.movementInput, 0.9).guessBestMovement(getRiddenSpeed(player), player);
    }

    public float getRiddenSpeed(GrimPlayer player) {
        PacketEntityNautilus nautilus = (PacketEntityNautilus) player.compensatedEntities.self.getRiding();
        return player.wasTouchingWater
                ? 0.0325F * (float) nautilus.getAttributeValue(Attributes.MOVEMENT_SPEED)
                : 0.02F * (float) nautilus.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

}
