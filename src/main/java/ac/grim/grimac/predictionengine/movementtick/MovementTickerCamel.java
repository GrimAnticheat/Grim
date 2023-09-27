package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;

public class MovementTickerCamel extends MovementTickerHorse {

    public MovementTickerCamel(GrimPlayer player) {
        super(player);
    }

    @Override
    public float getExtraSpeed() {
        PacketEntityHorse horsePacket = (PacketEntityHorse) player.compensatedEntities.getSelf().getRiding();
        return player.compensatedEntities.hasSprintingAttributeEnabled && player.vehicleData.dashCooldown <= 0 && !horsePacket.isDashing ? 0.1f : 0.0f;
    }
}