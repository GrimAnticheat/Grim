package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;

public class MovementTickerCamel extends MovementTickerHorse {

    public MovementTickerCamel(GrimPlayer player) {
        super(player);
    }

    @Override
    public float getExtraSpeed() {
        return player.compensatedEntities.hasSprintingAttributeEnabled && player.vehicleData.dashCooldown == 0  ? 0.1f : 0.0f;
    }
}