package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.teleportaccept.WrappedPacketInTeleportAccept;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

public class PacketPlayerTeleport extends PacketListenerDynamic {

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.TELEPORT_ACCEPT) {
            WrappedPacketInTeleportAccept accept = new WrappedPacketInTeleportAccept(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            Vector3d teleportLocation = player.teleports.remove(accept.getTeleportId());
            byte relative = player.relative.remove(accept.getTeleportId());

            // Impossible under normal vanilla client
            if (teleportLocation == null) return;

            // Set the player's old location because pistons are glitchy
            player.packetLastTeleport = new Vector(player.lastX, player.lastY, player.lastZ);

            double teleportX = teleportLocation.getX();
            double teleportY = teleportLocation.getY();
            double teleportZ = teleportLocation.getZ();

            player.isJustTeleported = true;

            if ((relative & 1) == 1) {
                teleportX += player.lastX;
            } else {
                player.baseTickSetX(0);
            }

            if ((relative >> 1 & 1) == 1) {
                teleportY += player.lastY;
            } else {
                player.baseTickSetY(0);
            }

            if ((relative >> 2 & 1) == 1) {
                teleportZ += player.lastZ;
            } else {
                player.baseTickSetZ(0);
            }

            // Avoid setting the X Y and Z directly as that isn't thread safe
            player.packetTeleportX = teleportX;
            player.packetTeleportY = teleportY;
            player.packetTeleportZ = teleportZ;

            Bukkit.broadcastMessage("Teleport accepted!");
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {

        if (event.getPacketId() == PacketType.Play.Server.POSITION) {
            WrappedPacketOutPosition teleport = new WrappedPacketOutPosition(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            // This shouldn't be null unless another plugin is incorrectly using packets
            player.teleports.put(teleport.getTeleportId().get(), teleport.getPosition());
            player.relative.put(teleport.getTeleportId().get(), teleport.getRelativeFlagsMask());

            Bukkit.broadcastMessage("Teleporting to " + teleport.getPosition().toString());
        }
    }
}
