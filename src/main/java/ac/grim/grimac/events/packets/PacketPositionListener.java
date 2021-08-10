package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.in.steervehicle.WrappedPacketInSteerVehicle;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketPositionListener extends PacketListenerAbstract {

    public PacketPositionListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.POSITION) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d pos = position.getPosition();
            player.reach.handleMovement(player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
            player.packetStateData.didLastMovementIncludePosition = true;

            PredictionData data = new PredictionData(player, pos.getX(), pos.getY(), pos.getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, position.isOnGround());
            MovementCheckRunner.checkTeleportQueue(data);

            if (data.isJustTeleported || player.noFall.tickNoFall(data))
                position.setOnGround(false);

            if (MovementCheckRunner.processAndCheckMovementPacket(data))
                player.timerCheck.processMovementPacket();
            else
                event.setCancelled(true);
        }

        if (packetID == PacketType.Play.Client.POSITION_LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d pos = position.getPosition();
            player.reach.handleMovement(position.getYaw(), position.getPitch());
            player.packetStateData.didLastMovementIncludePosition = true;

            PredictionData data = new PredictionData(player, pos.getX(), pos.getY(), pos.getZ(), position.getYaw(), position.getPitch(), position.isOnGround());
            boolean wasTeleported = MovementCheckRunner.checkTeleportQueue(data);

            if (data.isJustTeleported || player.noFall.tickNoFall(data))
                position.setOnGround(false);

            // 1.17 clients can send a position look packet while in a vehicle when using an item because mojang
            // Teleports can override this behavior
            if (!wasTeleported && ((player.bukkitPlayer.isInsideVehicle() || player.vehicle != null)
                    && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17))) {
                return;
            }

            if (MovementCheckRunner.processAndCheckMovementPacket(data))
                player.timerCheck.processMovementPacket();
            else
                event.setCancelled(true);
        }

        if (packetID == PacketType.Play.Client.LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.reach.handleMovement(position.getYaw(), position.getPitch());
            player.packetStateData.didLastMovementIncludePosition = false;
            player.packetStateData.packetPlayerXRot = position.getYaw();
            player.packetStateData.packetPlayerYRot = position.getPitch();

            // This is a dummy packet when in a vehicle
            // The player vehicle status is sync'd to the netty thread, therefore pull from bukkit to avoid excess work
            if (player.bukkitPlayer.isInsideVehicle() || player.vehicle != null) {
                return;
            }

            player.timerCheck.processMovementPacket();

            if (position.isOnGround() != player.packetStateData.packetPlayerOnGround) {
                player.packetStateData.packetPlayerOnGround = !player.packetStateData.packetPlayerOnGround;
                player.packetStateData.didGroundStatusChangeWithoutPositionPacket = true;
            }
        }

        if (packetID == PacketType.Play.Client.FLYING) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.timerCheck.processMovementPacket();
            player.reach.handleMovement(player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);
            player.packetStateData.didLastMovementIncludePosition = false;

            if (position.isOnGround() != player.packetStateData.packetPlayerOnGround) {
                player.packetStateData.packetPlayerOnGround = !player.packetStateData.packetPlayerOnGround;
                player.packetStateData.didGroundStatusChangeWithoutPositionPacket = true;
            }
        }

        if (packetID == PacketType.Play.Client.STEER_VEHICLE) {
            WrappedPacketInSteerVehicle steer = new WrappedPacketInSteerVehicle(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            // Multiple steer vehicles in a row, the player is not in control of their vehicle
            if (player.packetStateData.receivedSteerVehicle && player.vehicle != null) {
                MovementCheckRunner.processAndCheckMovementPacket(new PredictionData(player));
            } else {
                // Try and get the player's vehicle to the queue
                MovementCheckRunner.runTransactionQueue(player);
            }

            player.packetStateData.receivedSteerVehicle = true;

            player.packetStateData.packetVehicleForward = steer.getForwardValue();
            player.packetStateData.packetVehicleHorizontal = steer.getSideValue();
        }
    }
}