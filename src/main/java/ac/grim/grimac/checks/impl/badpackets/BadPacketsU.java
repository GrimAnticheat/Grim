package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsU")
public class BadPacketsU extends Check implements PacketCheck {

    private WrapperPlayClientPlayerFlying lastMovement;

    public BadPacketsU(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        //Screw c06 exploit, thanks a lot wykt! No more sprint scaffold

        //We are not checking for player.packetStateData.lastPacketWasOnePointSeventeenDuplicate because that just completly ignore it!
        //Player still sending c06 while eating, motionXZ check was really helpful. Thanks mojang!
        if (player.packetStateData.lastPacketWasTeleport || player.compensatedEntities.getSelf().getRiding() != null || player.packetStateData.slowedByUsingItem
        || player.wasTouchingWater) {
            return;
        }

        if (event.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerFlying flyingPacketWrapper = new WrapperPlayClientPlayerFlying(event);

            //Terrible check?, it works lol
            if(lastMovement != null) {
                if(shouldFlag(lastMovement.getLocation(), flyingPacketWrapper.getLocation())) {
                    event.setCancelled(true);
                    flagAndAlert();

                    player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                }
            }

            lastMovement = flyingPacketWrapper;
        }
    }

    private boolean shouldFlag(Location prev, Location current) {
        if(prev.getYaw() == current.getYaw() && prev.getPitch() == current.getPitch() ||
                prev.getX() == current.getX() && prev.getY() == current.getY() && prev.getZ() == current.getZ()) {
            return true;
        }

        return false;
    }

}