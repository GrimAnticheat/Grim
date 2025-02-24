package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityCamel;

public class MovementTickerCamel extends MovementTickerHorse {

    public MovementTickerCamel(GrimPlayer player) {
        super(player);
    }

    @Override
    public float getExtraSpeed() {
        PacketEntityCamel camel = (PacketEntityCamel) player.compensatedEntities.self.getRiding();

        // If jumping... speed wouldn't apply after this
        // This engine was not designed for this edge case
        final boolean wantsToJump = player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround;
        if (wantsToJump) return 0;

        return player.isSprinting && player.vehicleData.camelDashCooldown <= 0 && !camel.dashing ? 0.1f : 0.0f;
    }
}
