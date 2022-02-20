package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

public class MovementTickerHorse extends MovementTickerLivingVehicle {

    public MovementTickerHorse(GrimPlayer player) {
        super(player);

        PacketEntityHorse horsePacket = (PacketEntityHorse) player.playerVehicle;

        if (!horsePacket.hasSaddle) return;

        player.speed = horsePacket.movementSpeedAttribute;

        // Setup player inputs
        float f = player.vehicleData.vehicleHorizontal * 0.5F;
        float f1 = player.vehicleData.vehicleForward;

        if (f1 <= 0.0F) {
            f1 *= 0.25F;
        }

        this.movementInput = new Vector(f, 0, f1);
        if (movementInput.lengthSquared() > 1) movementInput.normalize();
    }

    @Override
    public void livingEntityAIStep() {
        super.livingEntityAIStep();
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) Collisions.handleInsideBlocks(player);
    }
}
