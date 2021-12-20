package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.JumpPower;
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

        // If the player wants to jump on a horse
        // Listen to Entity Action -> start jump with horse, stop jump with horse
        if (player.vehicleData.horseJump > 0.0F && !player.vehicleData.horseJumping && player.lastOnGround) {
            // Safe to use attributes as entity riding is server sided on 1.8
            // Not using bukkit API getJumpStrength() because the API changes around 1.11
            double d0 = horsePacket.jumpStrength * player.vehicleData.horseJump * JumpPower.getPlayerJumpFactor(player);
            double d1;

            // This doesn't even work because vehicle jump boost has (likely) been
            // broken ever since vehicle control became client sided
            //
            // But plugins can still send this, so support it anyways
            if (player.compensatedPotions.getJumpAmplifier() != null) {
                d1 = d0 + ((player.compensatedPotions.getJumpAmplifier() + 1) * 0.1F);
            } else {
                d1 = d0;
            }

            player.clientVelocity.setY(d1 / 0.98);
            player.vehicleData.horseJumping = true;

            if (f1 > 0.0F) {
                float f2 = player.trigHandler.sin(player.xRot * ((float) Math.PI / 180F));
                float f3 = player.trigHandler.cos(player.xRot * ((float) Math.PI / 180F));
                player.baseTickAddVector(new Vector(-0.4F * f2 * player.vehicleData.horseJump, 0.0D, 0.4F * f3 * player.vehicleData.horseJump).multiply(1 / 0.98));
            }

            player.vehicleData.horseJump = 0.0F;
        }

        // More jumping stuff
        if (player.lastOnGround) {
            player.vehicleData.horseJump = 0.0F;
            player.vehicleData.horseJumping = false;
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
