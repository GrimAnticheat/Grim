package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.Pose;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {

    public MovementTickerStrider(GrimPlayer player) {
        super(player);

        if (player.playerVehicle.pose == Pose.DYING) {
            player.clientVelocity = new Vector();
            return;
        }

        ((PacketEntityStrider) player.playerVehicle).isShaking = true;
        // Blocks are stored in YZX order
        checkLava:
        {
            for (int y = (int) Math.floor(player.boundingBox.minY); y < Math.ceil(player.boundingBox.maxY); y++) {
                for (int z = (int) Math.floor(player.boundingBox.minZ) - 1; z < Math.ceil(player.boundingBox.maxZ); z++) {
                    for (int x = (int) Math.floor(player.boundingBox.minX); x < Math.ceil(player.boundingBox.maxX); x++) {
                        if (player.compensatedWorld.getLavaFluidLevelAt(x, y, z) > 0) {
                            ((PacketEntityStrider) player.playerVehicle).isShaking = false;
                            break checkLava;
                        }
                    }
                }
            }
        }

        movementInput = new Vector(0, 0, player.speed);
    }

    @Override
    public float getSteeringSpeed() { // Don't question why we have to multiply by 10
        PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;
        return strider.movementSpeedAttribute * (strider.isShaking ? 0.23F : 0.55F) * 10f;
    }

    @Override
    public void livingEntityTravel() {
        floatStrider();

        super.livingEntityTravel();
    }

    private void floatStrider() {
        if (player.wasTouchingLava) {
            if (isAbove() && player.compensatedWorld.
                    getLavaFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(player.lastY + 1), (int) Math.floor(player.lastZ)) == 0) {
                player.uncertaintyHandler.striderOnGround = true;
                // This is a hack because I believe there is something wrong with order of collision stuff.
                // that doesn't affect players but does affect things that artificially change onGround status
                player.clientVelocity.setY(0);
            } else {
                player.clientVelocity.multiply(0.5).add(new Vector(0, 0.05, 0));
                player.uncertaintyHandler.striderOnGround = false;
            }
        } else {
            player.uncertaintyHandler.striderOnGround = false;
        }
    }

    public boolean isAbove() {
        return player.lastY > Math.floor(player.lastY) + 0.5 - (double) 1.0E-5F;
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
