package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Strider;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {
    public MovementTickerStrider(GrimPlayer player) {
        super(player);

        movementInput = new Vector(0, 0, 1);
    }

    public float getSteeringSpeed() {
        PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;
        float speed =  (float) PredictionData.getMovementSpeedAttribute((LivingEntity) strider.entity);

        return speed * (strider.isShaking ? 0.66F : 1.0F);
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
