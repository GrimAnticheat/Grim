package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Location;

public class PacketServerTeleport extends PacketListenerAbstract {

    public PacketServerTeleport() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.POSITION) {
            WrappedPacketOutPosition teleport = new WrappedPacketOutPosition(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

            byte relative = teleport.getRelativeFlagsMask();
            Vector3d pos = teleport.getPosition();
            float pitch = teleport.getPitch();
            float yaw = teleport.getYaw();

            if (player == null) {
                // Player teleport event gets called AFTER player join event
                new GrimPlayer(event.getPlayer());
                player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
                if (player == null) return; // This player is exempt from all checks
            }

            // Convert relative teleports to normal teleports
            // We have to do this because 1.8 players on 1.9+ get teleports changed by ViaVersion
            // Additionally, velocity is kept after relative teleports making predictions difficult
            // The added complexity isn't worth a feature that I have never seen used
            //
            // If you do actually need this make an issue on GitHub with an explanation for why
            if ((relative & 1) == 1)
                pos = pos.add(new Vector3d(player.packetStateData.packetPosition.x, 0, 0));

            if ((relative >> 1 & 1) == 1)
                pos = pos.add(new Vector3d(0, player.packetStateData.packetPosition.y, 0));

            if ((relative >> 2 & 1) == 1)
                pos = pos.add(new Vector3d(0, 0, player.packetStateData.packetPosition.z));

            teleport.setPosition(pos);

            if (teleport.getPitch() == 12.419510391f && teleport.getYaw() == 41.12315918f) {
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8)) { // ViaVersion converts relative teleports
                    teleport.setYaw(player.packetStateData.lastPacketPlayerXRot);
                    teleport.setPitch(player.packetStateData.lastPacketPlayerYRot);
                    teleport.setRelativeFlagsMask((byte) 0);
                } else {
                    teleport.setYaw(0);
                    teleport.setPitch(0);
                    teleport.setRelativeFlagsMask((byte) 0b11000);
                }
            } else {
                teleport.setYaw(yaw);
                teleport.setPitch(pitch);
                teleport.setRelativeFlagsMask((byte) 0);
            }

            player.sendTransaction();
            final int lastTransactionSent = player.lastTransactionSent.get();
            event.setPostTask(player::sendTransaction);

            // For some reason teleports on 1.7 servers are offset by 1.62?
            if (ServerVersion.getVersion().isOlderThan(ServerVersion.v_1_8))
                pos.setY(pos.getY() - 1.62);

            Location target = new Location(player.bukkitPlayer.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            boolean cancel = player.getSetbackTeleportUtil().addSentTeleport(target, lastTransactionSent);

            // It's the damn vanilla anticheat again!  We must override it!
            if (cancel) {
                event.setCancelled(true);
            }
        }

        if (packetID == PacketType.Play.Server.VEHICLE_MOVE) {
            WrappedPacket vehicleMove = new WrappedPacket(event.getNMSPacket());
            double x = vehicleMove.readDouble(0);
            double y = vehicleMove.readDouble(1);
            double z = vehicleMove.readDouble(2);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            player.sendTransaction();
            int lastTransactionSent = player.lastTransactionSent.get();
            Vector3d finalPos = new Vector3d(x, y, z);

            event.setPostTask(player::sendTransaction);
            player.vehicleData.vehicleTeleports.add(new Pair<>(lastTransactionSent, finalPos));
        }
    }
}
