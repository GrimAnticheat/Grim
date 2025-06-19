package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHappyGhast;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerHappyGhast extends MovementTickerLivingVehicle {

    public MovementTickerHappyGhast(GrimPlayer player) {
        super(player);

        // TODO: check if this is right
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        player.speed = happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) + getExtraSpeed();

        // Setup player inputs
        float forward = player.vehicleData.vehicleHorizontal;
        float sideways = 0.0F;
        float upAndDown = 0.0F;
        if (player.vehicleData.vehicleForward != 0.0F) {
            float calcSideways = player.trigHandler.cos(player.xRot * (float) (Math.PI / 180.0));
            float calcUpAndDown = -player.trigHandler.sin(player.xRot * (float) (Math.PI / 180.0));
            if (player.vehicleData.vehicleForward < 0.0F) {
                calcSideways *= -0.5F;
                calcUpAndDown *= -0.5F;
            }

            upAndDown = calcUpAndDown;
            sideways = calcSideways;
        }

        if (player.packetStateData.knownInput.jump()) {
            upAndDown += 0.5F;
        }

        this.movementInput = new Vector3dm(forward, upAndDown, sideways).multiply(3.9F * happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED));
        if (movementInput.lengthSquared() > 1) movementInput.normalize();
    }

    // TODO: implement movement

    @Override
    public void livingEntityAIStep() {
        super.livingEntityAIStep();
    }

    public float getExtraSpeed() {
        return 0f;
    }
}
