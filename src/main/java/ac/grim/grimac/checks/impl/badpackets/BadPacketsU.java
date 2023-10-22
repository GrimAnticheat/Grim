package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsU")
public class BadPacketsU extends Check implements PacketCheck {

    private int positionUpdateTicks = 0, lastUse = 0;

    private WrapperPlayClientPlayerFlying lastMovement;

    public BadPacketsU(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        //Screw c06 exploit, thanks a lot wykt! No more sprint scaffold

        //We are not checking for player.packetStateData.lastPacketWasOnePointSeventeenDuplicate because that just completly ignore it!
        //Player still sending c06 while eating, posXZ check was really helpful. Thanks mojang!
        //Also swimming player false flagging, that nice
//        if (player.packetStateData.lastPacketWasTeleport || player.compensatedEntities.getSelf().getRiding() != null || player.packetStateData.slowedByUsingItem
//        || player.isSwimming || !player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
//            return;
//        }

        //So currently spamming right click flag, I love mojang so much

        if(event.getPacketType() == Client.ENTITY_ACTION) {
            //System.out.println("Uh!");
            lastUse = 5;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                //System.out.println("Uh!");
                lastUse = 5;
            }
        }

        if (player.packetStateData.lastPacketWasTeleport || player.compensatedEntities.getSelf().getRiding() != null || player.isSwimming ||
                !player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) || lastUse > 0 || player.packetStateData.slowedByUsingItem) {
            if(lastUse > 0) lastUse--;
            return;
        }

        if (event.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerFlying flyingPacketWrapper = new WrapperPlayClientPlayerFlying(event);

            boolean flag2 = true, flag3;

            if(lastMovement != null) {
                //Alright so since people keep saying it gonna false flag, let do it the same as vanilla
                double d0 = flyingPacketWrapper.getLocation().getX() - lastMovement.getLocation().getX();
                double d1 = flyingPacketWrapper.getLocation().getY() - lastMovement.getLocation().getY();
                double d2 = flyingPacketWrapper.getLocation().getZ() - lastMovement.getLocation().getZ();
                double d3 = flyingPacketWrapper.getLocation().getYaw() - lastMovement.getLocation().getYaw();
                double d4 = flyingPacketWrapper.getLocation().getPitch() - lastMovement.getLocation().getPitch();
                flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
                flag3 = d3 != 0.0D || d4 != 0.0D;

                if(!flag2 || !flag3) {
                    //Just making sure we not gonna false
                    if(shouldFlag(lastMovement.getLocation(), flyingPacketWrapper.getLocation())
                    && !isPositionTheSame(lastMovement.getLocation(), flyingPacketWrapper.getLocation())) {
                        event.setCancelled(true);

                        flagAndAlert();

                        player.getSetbackTeleportUtil().executeViolationSetback();
                    }
                }
            }

            positionUpdateTicks++;

            if(flag2) {
                positionUpdateTicks = 0;
            }
        }

        if(event.getPacketType() == Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == Client.PLAYER_POSITION ||
                event.getPacketType() == Client.PLAYER_ROTATION) {
            lastMovement = new WrapperPlayClientPlayerFlying(event);
        }
    }

    private boolean shouldFlag(Location prev, Location current) {
        if(prev.getYaw() == current.getYaw() && prev.getPitch() == current.getPitch() ||
                prev.getX() == current.getX() && prev.getY() == current.getY() && prev.getZ() == current.getZ()) {
            return true;
        }

        return false;
    }

    private boolean isPositionTheSame(Location prev, Location current) {
        if(prev.getYaw() == current.getYaw() && prev.getPitch() == current.getPitch() &&
                prev.getX() == current.getX() && prev.getY() == current.getY() && prev.getZ() == current.getZ()) {
            return true;
        }

        return false;
    }

}