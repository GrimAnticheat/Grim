package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.KnownInput;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;

public class PacketPlayerSteer extends PacketListenerAbstract {

    public PacketPlayerSteer() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientSteerVehicle steer = new WrapperPlayClientSteerVehicle(event);

            float forwards = steer.getForward();
            float sideways = steer.getSideways();

            player.vehicleData.nextVehicleForward = forwards;
            player.vehicleData.nextVehicleHorizontal = sideways;

            PacketEntity riding = player.compensatedEntities.self.getRiding();

            // Multiple steer vehicles in a row, the player is not in control of their vehicle
            // We must do this SYNC! to netty, as to get the packet location of the vehicle
            // Otherwise other checks may false because the player's position is unknown.
            if (player.packetStateData.receivedSteerVehicle && riding != null) {
                // Horse and boat have first passenger in control
                // If the player is the first passenger, disregard this attempt to have the server control the entity
                if ((riding.isBoat() || riding instanceof PacketEntityHorse) && riding.passengers.get(0) == player.compensatedEntities.self &&
                        // Although if the player has server controlled entities
                        player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) &&
                        // or the server controls the entities, then this is vanilla logic so allow it
                        PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                    return;
                }

                // Tick update
                player.compensatedWorld.tickPlayerInPistonPushingArea();
                player.compensatedEntities.tick();

                // Note for the movement check
                player.vehicleData.lastDummy = true;

                // Update knockback and explosions after getting the vehicle
                int controllingEntityId = player.inVehicle() ? player.getRidingVehicleId() : player.entityID;
                player.firstBreadKB = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(controllingEntityId, player.lastTransactionReceived.get());
                player.likelyKB = player.checkManager.getKnockbackHandler().calculateRequiredKB(controllingEntityId, player.lastTransactionReceived.get(), false);

                // The player still applies kb even if they aren't in control of the vehicle, for some reason
                if (player.firstBreadKB != null) {
                    player.clientVelocity = player.firstBreadKB.vector;
                }
                if (player.likelyKB != null) {
                    player.clientVelocity = player.likelyKB.vector;
                }

                player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
                player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get(), false);

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

                SimpleCollisionBox vehiclePos = player.compensatedEntities.self.getRiding().getPossibleCollisionBoxes();

                player.x = (vehiclePos.minX + vehiclePos.maxX) / 2;
                player.y = (vehiclePos.minY + vehiclePos.maxY) / 2;
                player.z = (vehiclePos.minZ + vehiclePos.maxZ) / 2;

                if (player.isSprinting != player.lastSprinting) {
                    player.compensatedEntities.hasSprintingAttributeEnabled = player.isSprinting;
                }
                player.lastSprinting = player.isSprinting;
            }

            player.packetStateData.receivedSteerVehicle = true;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientPlayerInput input = new WrapperPlayClientPlayerInput(event);
            byte forward = 0;
            byte sideways = 0;
            if (input.isForward()) {
                forward++;
            }

            if (input.isBackward()) {
                forward--;
            }

            if (input.isLeft()) {
                sideways++;
            }

            if (input.isRight()) {
                sideways--;
            }

            player.vehicleData.nextVehicleForward = forward * 0.98f;
            player.vehicleData.nextVehicleHorizontal = sideways * 0.98f;

            player.packetStateData.knownInput = new KnownInput(input.isForward(), input.isBackward(), input.isLeft(), input.isRight(), input.isJump(), input.isShift(), input.isSprint());
        }
    }
}
