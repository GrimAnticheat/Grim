package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.steervehicle.WrappedPacketInSteerVehicle;

public class PacketPlayerSteer extends PacketListenerAbstract {

    public PacketPlayerSteer() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.STEER_VEHICLE) {
            WrappedPacketInSteerVehicle steer = new WrappedPacketInSteerVehicle(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            // Multiple steer vehicles in a row, the player is not in control of their vehicle
            // We must do this SYNC! to netty, as to get the packet location of the vehicle
            // Otherwise other checks may false because the player's position is unknown.
            if (player.tasksNotFinished.get() == 0 && player.packetStateData.receivedSteerVehicle && player.playerVehicle != null) {
                player.lastTransactionReceived = player.packetStateData.packetLastTransactionReceived.get();

                // Tick updates AFTER updating bounding box and actual movement
                player.compensatedWorld.tickUpdates(player.lastTransactionReceived);
                player.compensatedWorld.tickPlayerInPistonPushingArea();

                // Stop transaction leaks
                player.latencyUtils.handleAnticheatSyncTransaction(player.lastTransactionReceived);

                // Update entities to get current vehicle
                player.compensatedEntities.tickUpdates(player.packetStateData.packetLastTransactionReceived.get());

                // Not responsible for applying knockback/explosions
                player.checkManager.getExplosionHandler().handlePlayerExplosion(0, true);
                player.checkManager.getKnockbackHandler().handlePlayerKb(0, true);

                // Note for the movement check
                player.vehicleData.lastDummy = true;

                // Keep a reference of this just in case the next like sets this to null
                PacketEntity vehicle = player.playerVehicle;

                // Tick player vehicle after we update the packet entity state
                player.lastVehicle = player.playerVehicle;
                player.playerVehicle = player.vehicle == null ? null : player.compensatedEntities.getEntity(player.vehicle);
                player.inVehicle = player.playerVehicle != null;

                // Set position now to support "dummy" riding without control
                // Warning - on pigs and striders players, can turn into dummies independent of whether they have
                // control of the vehicle or not (which could be abused to set velocity to 0 repeatedly and kind
                // of float in the air, although what's the point inside a vehicle?)
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;

                player.x = vehicle.position.getX();
                player.y = vehicle.position.getY();
                player.z = vehicle.position.getZ();

                player.packetStateData.packetPosition = vehicle.position;

                return;
            } else {
                // Try and get the player's vehicle to the queue for next time
                MovementCheckRunner.runTransactionQueue(player);
            }

            player.packetStateData.receivedSteerVehicle = true;

            player.packetStateData.packetVehicleForward = steer.getForwardValue();
            player.packetStateData.packetVehicleHorizontal = steer.getSideValue();
        }
    }

}
