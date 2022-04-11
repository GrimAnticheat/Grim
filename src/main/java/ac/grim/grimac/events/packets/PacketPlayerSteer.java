package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class PacketPlayerSteer extends PacketListenerAbstract {

    public PacketPlayerSteer() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            WrapperPlayClientSteerVehicle steer = new WrapperPlayClientSteerVehicle(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // Multiple steer vehicles in a row, the player is not in control of their vehicle
            // We must do this SYNC! to netty, as to get the packet location of the vehicle
            // Otherwise other checks may false because the player's position is unknown.
            if (player.packetStateData.receivedSteerVehicle && player.playerVehicle != null) {
                // Tick update
                player.compensatedWorld.tickPlayerInPistonPushingArea();
                player.compensatedEntities.tick();

                // Note for the movement check
                player.vehicleData.lastDummy = true;

                // Update knockback and explosions after getting the vehicle
                player.firstBreadKB = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());
                player.likelyKB = player.checkManager.getKnockbackHandler().calculateRequiredKB(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());

                // The player still applies kb even if they aren't in control of the vehicle, for some reason
                if (player.firstBreadKB != null) {
                    player.clientVelocity = player.firstBreadKB.vector;
                }
                if (player.likelyKB != null) {
                    player.clientVelocity = player.likelyKB.vector;
                }

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

                SimpleCollisionBox vehiclePos = player.playerVehicle.getPossibleCollisionBoxes();

                player.x = (vehiclePos.minX + vehiclePos.maxX) / 2;
                player.y = (vehiclePos.minY + vehiclePos.maxY) / 2;
                player.z = (vehiclePos.minZ + vehiclePos.maxZ) / 2;

                if (player.bukkitPlayer == null) return;

                // Use bukkit location, not packet location, to stop ping spoof attacks on entity position
                Entity playerVehicle = player.bukkitPlayer.getVehicle();
                if (playerVehicle != null) {
                    Location location = playerVehicle.getLocation();
                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();
                    player.getSetbackTeleportUtil().setSafeSetbackLocation(new Vector3d(x, y, z));
                }

                return;
            }

            player.packetStateData.receivedSteerVehicle = true;

            float forwards = steer.getForward();
            float sideways = steer.getSideways();

            player.vehicleData.nextVehicleForward = forwards;
            player.vehicleData.nextVehicleHorizontal = sideways;
        }
    }
}
