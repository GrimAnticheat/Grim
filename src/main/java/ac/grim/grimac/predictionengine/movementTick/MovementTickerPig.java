package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.enums.Pose;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(GrimPlayer player) {
        super(player);

        if (player.playerVehicle.pose == Pose.DYING) {
            player.clientVelocity = new Vector();
            return;
        }

        movementInput = new Vector(0, 0, 1);
    }

    public float getSteeringSpeed() { // Not sure why the * 0.225 is needed
        Entity pig = player.playerVehicle.entity;
        return (float) PredictionData.getMovementSpeedAttribute((LivingEntity) pig) * 0.225f;
    }

    @Override
    public boolean isPig() {
        return true;
    }
}
