package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.in.steervehicle.WrappedPacketInSteerVehicle;
import io.github.retrooper.packetevents.packetwrappers.play.in.vehiclemove.WrappedPacketInVehicleMove;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketVehicleMoves extends PacketListenerAbstract {

    public PacketVehicleMoves() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.VEHICLE_MOVE) {
            WrappedPacketInVehicleMove move = new WrappedPacketInVehicleMove(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.packetStateData.receivedSteerVehicle = false;
            Vector3d pos = move.getPosition();

            PredictionData data = new PredictionData(player, pos.getX(), pos.getY(), pos.getZ(), move.getYaw(), move.getPitch());
            MovementCheckRunner.checkVehicleTeleportQueue(data);

            player.timerCheck.processMovementPacket();

            MovementCheckRunner.processAndCheckMovementPacket(data);
        }

        if (packetID == PacketType.Play.Client.STEER_VEHICLE) {
            WrappedPacketInSteerVehicle steer = new WrappedPacketInSteerVehicle(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            // Multiple steer vehicles in a row, the player is not in control of their vehicle
            // We must do this SYNC! to netty, as to get the packet location of the vehicle
            // Otherwise other checks may false because the player's position is unknown.
            if (player.tasksNotFinished.get() == 0 && player.packetStateData.receivedSteerVehicle && player.vehicle != null) {
                player.lastTransactionReceived = player.packetStateData.packetLastTransactionReceived.get();

                // Tick updates AFTER updating bounding box and actual movement
                player.compensatedWorld.tickUpdates(player.lastTransactionReceived);
                player.compensatedWorld.tickPlayerInPistonPushingArea();

                // Stop transaction leaks
                player.latencyUtils.handleAnticheatSyncTransaction(player.lastTransactionReceived);

                // Update entities to get current vehicle
                player.compensatedEntities.tickUpdates(player.packetStateData.packetLastTransactionReceived.get(), true);

                // Note for the movement check
                player.lastDummy = true;

                // Tick player vehicle after we update the packet entity state
                player.lastVehicle = player.playerVehicle;
                player.playerVehicle = player.vehicle == null ? null : player.compensatedEntities.getEntity(player.vehicle);
                player.inVehicle = player.playerVehicle != null;

                player.firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived);
                player.likelyKB = player.knockbackHandler.getRequiredKB(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived);

                player.firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion(player.lastTransactionReceived);
                player.likelyExplosions = player.explosionHandler.getPossibleExplosions(player.lastTransactionReceived);

                // Players are unable to take explosions in vehicles
                player.explosionHandler.handlePlayerExplosion(0, true);
                // Players not in control of their vehicle are not responsible for applying knockback to it
                player.knockbackHandler.handlePlayerKb(0, true);

                // Set position now to support "dummy" riding without control
                // Warning - on pigs and striders players, can turn into dummies independent of whether they have
                // control of the vehicle or not (which could be abused to set velocity to 0 repeatedly and kind
                // of float in the air, although what's the point inside a vehicle?)
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;

                player.x = player.playerVehicle.position.getX();
                player.y = player.playerVehicle.position.getY();
                player.z = player.playerVehicle.position.getZ();

                player.packetStateData.packetPlayerX = player.x;
                player.packetStateData.packetPlayerY = player.y;
                player.packetStateData.packetPlayerZ = player.z;

                return;
            } else {
                // Try and get the player's vehicle to the queue
                MovementCheckRunner.runTransactionQueue(player);
            }

            player.packetStateData.receivedSteerVehicle = true;

            player.packetStateData.packetVehicleForward = steer.getForwardValue();
            player.packetStateData.packetVehicleHorizontal = steer.getSideValue();
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.VEHICLE_MOVE) {
            WrappedPacket vehicleMove = new WrappedPacket(event.getNMSPacket());
            double x = vehicleMove.readDouble(0);
            double y = vehicleMove.readDouble(1);
            double z = vehicleMove.readDouble(2);

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            Vector3d finalPos = new Vector3d(x, y, z);

            event.setPostTask(player::sendAndFlushTransactionOrPingPong);
            player.vehicleTeleports.add(new Pair<>(lastTransactionSent, finalPos));
        }
    }
}
