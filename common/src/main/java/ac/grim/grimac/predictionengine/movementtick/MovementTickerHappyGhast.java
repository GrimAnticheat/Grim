package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineHappyGhast;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHappyGhast;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerHappyGhast extends MovementTickerLivingVehicle {

    public MovementTickerHappyGhast(GrimPlayer player) {
        super(player);

        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        if (!happyGhastPacket.isControllingPassenger()) return;

        player.speed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;

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

        if (player.lastJumping) {
            upAndDown += 0.5F;
        }

        this.movementInput = new Vector3dm(sideways, upAndDown, forward).multiply(3.9F * happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED));
        if (this.movementInput.lengthSquared() > 1) this.movementInput.normalize();
    }

    @Override
    public void doNormalMove(float blockFriction) {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(this.movementInput, 0.91F).guessBestMovement(flyingSpeed, player);
    }

    @Override
    public void doLavaMove() {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(this.movementInput, 0.5).guessBestMovement(flyingSpeed, player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(this.movementInput, 0.8F).guessBestMovement(flyingSpeed, player);
    }

}
