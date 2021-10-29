package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.steervehicle.WrappedPacketInSteerVehicle;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;

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
            if (player.packetStateData.receivedSteerVehicle && player.playerVehicle != null) {
                // Tick updates AFTER updating bounding box and actual movement
                player.compensatedWorld.tickPlayerInPistonPushingArea();

                // Note for the movement check
                player.vehicleData.lastDummy = true;

                // Keep a reference of this just in case the next like sets this to null
                PacketEntity vehicle = player.playerVehicle;

                // Tick player vehicle after we update the packet entity state
                player.lastVehicle = player.playerVehicle;
                player.playerVehicle = player.vehicle == null ? null : player.compensatedEntities.getEntity(player.vehicle);
                player.inVehicle = player.playerVehicle != null;

                // Update knockback and explosions after getting the vehicle
                player.firstBreadKB = player.checkManager.getKnockbackHandler().getFirstBreadOnlyKnockback(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());
                player.likelyKB = player.checkManager.getKnockbackHandler().getRequiredKB(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());

                player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
                player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get());

                // Not responsible for applying knockback/explosions
                player.checkManager.getExplosionHandler().forceExempt();
                player.checkManager.getKnockbackHandler().forceExempt();

                // Set position now to support "dummy" riding without control
                // Warning - on pigs and striders players, can turn into dummies independent of whether they have
                // control of the vehicle or not (which could be abused to set velocity to 0 repeatedly and kind
                // of float in the air, although what's the point inside a vehicle?)
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;

                SimpleCollisionBox vehiclePos = vehicle.getPossibleCollisionBoxes();

                player.x = (vehiclePos.minX + vehiclePos.maxX) / 2;
                player.y = (vehiclePos.minY + vehiclePos.maxY) / 2;
                player.z = (vehiclePos.minZ + vehiclePos.maxZ) / 2;

                // Use bukkit location, not packet location, to stop ping spoof attacks on entity position
                Entity playerVehicle = player.bukkitPlayer.getVehicle();
                if (playerVehicle != null) {
                    double x = playerVehicle.getLocation().getX();
                    double y = playerVehicle.getLocation().getY();
                    double z = playerVehicle.getLocation().getZ();
                    player.getSetbackTeleportUtil().setSafeSetbackLocation(player.bukkitPlayer.getWorld(), new Vector3d(x, y, z));
                }

                return;
            } else {
                // Try and get the player's vehicle to the queue for next time
                player.movementCheckRunner.runTransactionQueue(player);
            }

            player.packetStateData.receivedSteerVehicle = true;

            player.vehicleData.nextVehicleForward = steer.getForwardValue();
            player.vehicleData.nextVehicleForward = steer.getSideValue();
        }
    }
}
