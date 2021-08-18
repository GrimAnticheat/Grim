package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.TeleportUtil;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.in.vehiclemove.WrappedPacketInVehicleMove;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class CheckManagerListener extends PacketListenerAbstract {

    long lastPosLook = 0;

    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());

            boolean hasPosition = packetID == PacketType.Play.Client.POSITION || packetID == PacketType.Play.Client.POSITION_LOOK;
            boolean hasLook = packetID == PacketType.Play.Client.LOOK || packetID == PacketType.Play.Client.POSITION_LOOK;

            // Don't check duplicate 1.17 packets (Why would you do this mojang?)
            // Don't check rotation since it changes between these packets, with the second being irrelevant.
            if (hasPosition && hasLook) {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17) && System.currentTimeMillis() - lastPosLook < 750 &&
                        player.packetStateData.packetPosition.equals(flying.getPosition())) {
                    lastPosLook = System.currentTimeMillis();
                    return;
                }
            }

            lastPosLook = System.currentTimeMillis();

            if (!hasPosition && flying.isOnGround() != player.packetStateData.packetPlayerOnGround)
                player.packetStateData.didGroundStatusChangeWithoutPositionPacket = true;

            player.packetStateData.lastPacketPlayerXRot = player.packetStateData.packetPlayerXRot;
            player.packetStateData.lastPacketPlayerYRot = player.packetStateData.packetPlayerYRot;
            player.packetStateData.lastPacketPosition = player.packetStateData.packetPosition;
            player.packetStateData.lastPacketWasTeleport = false;
            player.packetStateData.packetPlayerOnGround = flying.isOnGround();

            if (hasLook) {
                float xRot = flying.getYaw();
                float yRot = flying.getPitch();

                player.packetStateData.packetPlayerXRot = xRot;
                player.packetStateData.packetPlayerYRot = yRot;
            }

            if (hasPosition) {
                Vector3d position = flying.getPosition();
                player.packetStateData.packetPosition = position;

                final boolean isTeleport = TeleportUtil.checkTeleportQueue(player, position.getX(), position.getY(), position.getZ());
                player.packetStateData.lastPacketWasTeleport = isTeleport;

                final PositionUpdate update = new PositionUpdate(player.packetStateData.lastPacketPosition, position, flying.isOnGround(), isTeleport);
                player.checkManager.onPositionUpdate(update);
            }

            if (hasLook) {
                float xRot = flying.getYaw();
                float yRot = flying.getPitch();

                float deltaXRot = xRot - player.packetStateData.lastPacketPlayerXRot;
                float deltaYRot = yRot - player.packetStateData.lastPacketPlayerYRot;

                final RotationUpdate update = new RotationUpdate(player.packetStateData.lastPacketPlayerXRot, player.packetStateData.lastPacketPlayerYRot, xRot, yRot, deltaXRot, deltaYRot);
                player.checkManager.onRotationUpdate(update);
            }

            player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
            player.packetStateData.didLastMovementIncludePosition = hasPosition;
            player.packetStateData.movementPacketsReceived++;
        }

        if (packetID == PacketType.Play.Client.VEHICLE_MOVE) {
            WrappedPacketInVehicleMove move = new WrappedPacketInVehicleMove(event.getNMSPacket());
            Vector3d position = move.getPosition();

            final boolean isTeleport = TeleportUtil.checkVehicleTeleportQueue(player, position.getX(), position.getY(), position.getZ());
            final VehiclePositionUpdate update = new VehiclePositionUpdate(player.packetStateData.packetPosition, position, move.getYaw(), move.getPitch(), isTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
