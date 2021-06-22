package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.in.steervehicle.WrappedPacketInSteerVehicle;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketPositionListener extends PacketListenerAbstract {
    public PacketPositionListener() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.POSITION) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d pos = position.getPosition();

            MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player, pos.getX(), pos.getY(), pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, position.isOnGround()), false);
        }

        if (packetID == PacketType.Play.Client.POSITION_LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d pos = position.getPosition();

            MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player, pos.getX(), pos.getY(), pos.getZ(), position.getYaw(), position.getPitch(), position.isOnGround()), false);
        }

        if (packetID == PacketType.Play.Client.LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            // Prevent memory leaks from players continually staying in vehicles that they can't ride - also updates player position
            if (player.packetStateData.vehicle != null && player.compensatedEntities.entityMap.containsKey(player.packetStateData.vehicle)) {
                if (!player.packetStateData.receivedVehicleMove) {
                    MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player), true);
                }

                player.packetStateData.receivedVehicleMove = false;

                return;
            }

            MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player,
                    player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY, player.packetStateData.packetPlayerZ,
                    position.getYaw(), position.getPitch(), position.isOnGround()), false);
        }

        if (packetID == PacketType.Play.Client.FLYING) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player,
                    player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY, player.packetStateData.packetPlayerZ,
                    player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, position.isOnGround()), false);
        }

        if (packetID == PacketType.Play.Client.STEER_VEHICLE) {
            WrappedPacketInSteerVehicle steer = new WrappedPacketInSteerVehicle(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;
            player.packetStateData.packetVehicleForward = steer.getForwardValue();
            player.packetStateData.packetVehicleHorizontal = steer.getSideValue();
        }
    }
}