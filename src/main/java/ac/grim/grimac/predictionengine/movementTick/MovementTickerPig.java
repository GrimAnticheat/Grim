package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
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

    public float getSteeringSpeed() { // Not sure why the * 0.5625 is needed, don't question it.
        PacketEntityRideable pig = (PacketEntityRideable) player.playerVehicle;
        return pig.movementSpeedAttribute * 0.5625f;
    }

    @Override
    public boolean isPig() {
        return true;
    }
}
