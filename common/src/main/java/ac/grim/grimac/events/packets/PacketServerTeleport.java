package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.IntToObjectPair;
import ac.grim.grimac.utils.data.RotationData;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Location;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerVehicleMove;

public class PacketServerTeleport extends PacketListenerAbstract {

    private static final boolean STUPID_TELEPORT_SYSTEM = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);

    public PacketServerTeleport() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerPlayerPositionAndLook teleport = new WrapperPlayServerPlayerPositionAndLook(event);

            Vector3d pos = new Vector3d(teleport.getX(), teleport.getY(), teleport.getZ());

            // This is the first packet sent to the client which we need to track
            if (player.getSetbackTeleportUtil().getRequiredSetBack() == null) {
                // Player teleport event gets called AFTER player join event
                player.x = teleport.getX();
                player.y = teleport.getY();
                player.z = teleport.getZ();
                player.yaw = teleport.getYaw();
                player.pitch = teleport.getPitch();

                player.lastX = teleport.getX();
                player.lastY = teleport.getY();
                player.lastZ = teleport.getZ();
                player.lastYaw = teleport.getYaw();
                player.lastPitch = teleport.getPitch();

                player.pollData();
            }

            // 1.21.2+ client ignore teleports if player is inside vehicle, ABSOLUTE CINEMA MOJANG
            // cancel them as they are not doing anything and only can cause issues
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && player.compensatedEntities.serverPlayerVehicle != null) {
                event.setCancelled(true);
                return;
            }

            // Convert relative teleports to normal teleports
            // We have to do this because 1.8 players on 1.9+ get teleports changed by ViaVersion
            // Additionally, velocity is kept after relative teleports making predictions difficult
            // The added complexity isn't worth a feature that I have never seen used
            //
            // If you do actually need this make an issue on GitHub with an explanation for why
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) || player.inVehicle()) {
                boolean relativeX = teleport.isRelativeFlag(RelativeFlag.X),
                        relativeY = teleport.isRelativeFlag(RelativeFlag.Y),
                        relativeZ = teleport.isRelativeFlag(RelativeFlag.Z);

                if (relativeX) {
                    pos = pos.add(new Vector3d(player.x, 0, 0));
                    teleport.setRelative(RelativeFlag.X, false);
                }

                if (relativeY) {
                    pos = pos.add(new Vector3d(0, player.y, 0));
                    teleport.setRelative(RelativeFlag.Y, false);
                }

                if (relativeZ) {
                    pos = pos.add(new Vector3d(0, 0, player.z));
                    teleport.setRelative(RelativeFlag.Z, false);
                }

                if (relativeX || relativeY || relativeZ) {
                    teleport.setX(pos.getX());
                    teleport.setY(pos.getY());
                    teleport.setZ(pos.getZ());

                    event.markForReEncode(true);
                }
            }

            if (STUPID_TELEPORT_SYSTEM && player.inVehicle()) {
                boolean relativeDeltaX = teleport.isRelativeFlag(RelativeFlag.DELTA_X),
                        relativeDeltaY = teleport.isRelativeFlag(RelativeFlag.DELTA_Y),
                        relativeDeltaZ = teleport.isRelativeFlag(RelativeFlag.DELTA_Z);

                if (relativeDeltaX) {
                    teleport.setRelative(RelativeFlag.DELTA_X, false);
                }

                if (relativeDeltaY) {
                    teleport.setRelative(RelativeFlag.DELTA_Y, false);
                }

                if (relativeDeltaZ) {
                    teleport.setRelative(RelativeFlag.DELTA_Z, false);
                }

                if (relativeDeltaX || relativeDeltaY || relativeDeltaZ) {
                    teleport.setDeltaMovement(Vector3d.zero());
                    event.markForReEncode(true);
                }
            }

            player.sendTransaction();
            final int lastTransactionSent = player.lastTransactionSent.get();
            event.getTasksAfterSend().add(player::sendTransaction);

            if (teleport.isDismountVehicle()) {
                // Remove player from vehicle
                event.getTasksAfterSend().add(() -> player.compensatedEntities.self.eject());
            }

            // For some reason teleports on 1.7 servers are offset by 1.62?
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_8))
                pos = pos.withY(pos.getY() - 1.62);

            Location target = new Location(null, pos.getX(), pos.getY(), pos.getZ(), teleport.getYaw(), teleport.getPitch());
            player.getSetbackTeleportUtil().addSentTeleport(target, teleport.getDeltaMovement(), lastTransactionSent, teleport.getRelativeFlags(), true, teleport.getTeleportId());
        }

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ROTATION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerPlayerRotation packet = new WrapperPlayServerPlayerRotation(event);

            // I don't want to deal with this, so we'll prevent it
            if (!Float.isFinite(packet.getPitch())) {
                packet.setPitch(0);
                event.markForReEncode(true);
            }
            if (!Float.isFinite(packet.getYaw())) {
                packet.setYaw(0);
                event.markForReEncode(true);
            }

            player.sendTransaction();
            player.pendingRotations.add(new RotationData(packet.getYaw(), GrimMath.clamp(packet.getPitch() % 360F, -90F, 90F), player.getLastTransactionSent()));
            event.getTasksAfterSend().add(player::sendTransaction);
        }

        if (event.getPacketType() == PacketType.Play.Server.VEHICLE_MOVE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();
            event.getTasksAfterSend().add(player::sendTransaction);
            player.vehicleData.vehicleTeleports.add(new IntToObjectPair<>(
                    player.lastTransactionSent.get(),
                    new WrapperPlayServerVehicleMove(event).getPosition()
            ));
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2)) return;

            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(event);
            int entityId = teleport.getEntityId();

            if (player.vehicleData.removedPlayerVehicleId != null && player.vehicleData.removedPlayerVehicleId == entityId) {
                Vector3d pos = stripRelativeEntityTeleport(event, teleport, player.x, player.y, player.z);

                player.sendTransaction();
                final int lastTransactionSent = player.lastTransactionSent.get();
                event.getTasksAfterSend().add(player::sendTransaction);

                Location target = new Location(null, pos.getX(), pos.getY(), pos.getZ(), teleport.getYaw(), teleport.getPitch());
                player.getSetbackTeleportUtil().addSentTeleport(target, teleport.getDeltaMovement(), lastTransactionSent, teleport.getRelativeFlags(), true, 0);
                return;
            }

            if (player.compensatedEntities.serverPlayerVehicle != null && player.compensatedEntities.serverPlayerVehicle == entityId) {
                TrackerData data = player.compensatedEntities.getTrackedEntity(entityId);
                if (data == null) return;

                Vector3d pos = stripRelativeEntityTeleport(event, teleport, data.getX(), data.getY(), data.getZ());

                player.sendTransaction();
                event.getTasksAfterSend().add(player::sendTransaction);
                player.vehicleData.vehicleTeleports.add(new IntToObjectPair<>(
                        player.lastTransactionSent.get(),
                        pos
                ));
            }
        }
    }

    private Vector3d stripRelativeEntityTeleport(PacketSendEvent event, WrapperPlayServerEntityTeleport teleport, double baseX, double baseY, double baseZ) {
        Vector3d pos = teleport.getPosition();
        boolean relativeX = teleport.getRelativeFlags().has(RelativeFlag.X),
                relativeY = teleport.getRelativeFlags().has(RelativeFlag.Y),
                relativeZ = teleport.getRelativeFlags().has(RelativeFlag.Z);

        if (relativeX) {
            pos = pos.add(new Vector3d(baseX, 0, 0));
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.X, false));
        }

        if (relativeY) {
            pos = pos.add(new Vector3d(0, baseY, 0));
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.Y, false));
        }

        if (relativeZ) {
            pos = pos.add(new Vector3d(0, 0, baseZ));
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.Z, false));
        }

        if (relativeX || relativeY || relativeZ) {
            teleport.setPosition(pos);
            event.markForReEncode(true);
        }

        boolean relativeDeltaX = teleport.getRelativeFlags().has(RelativeFlag.DELTA_X),
                relativeDeltaY = teleport.getRelativeFlags().has(RelativeFlag.DELTA_Y),
                relativeDeltaZ = teleport.getRelativeFlags().has(RelativeFlag.DELTA_Z);

        if (relativeDeltaX) {
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.DELTA_X, false));
        }

        if (relativeDeltaY) {
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.DELTA_Y, false));
        }

        if (relativeDeltaZ) {
            teleport.setRelativeFlags(teleport.getRelativeFlags().set(RelativeFlag.DELTA_Z, false));
        }

        if (relativeDeltaX || relativeDeltaY || relativeDeltaZ) {
            teleport.setDeltaMovement(Vector3d.zero());
            event.markForReEncode(true);
        }

        return pos;
    }

}
