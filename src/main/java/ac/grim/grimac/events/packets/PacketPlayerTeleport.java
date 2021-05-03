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

public class PacketPlayerTeleport extends PacketListenerDynamic {

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.TELEPORT_ACCEPT) {
            WrappedPacketInTeleportAccept accept = new WrappedPacketInTeleportAccept(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            Vector3d teleportLocation = player.teleports.get(accept.getTeleportId());

            player.isJustTeleported = true;

            // A bit hacky but should be fine - set this stuff twice as optimization
            // Otherwise we will be running more scenarios to try and get the right velocity
            // Setting last coordinates here is necessary though, don't change that.
            player.lastX = teleportLocation.getX();
            player.lastY = teleportLocation.getY();
            player.lastZ = teleportLocation.getZ();
            player.baseTickSetX(0);
            player.baseTickSetY(0);
            player.baseTickSetZ(0);

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

            Bukkit.broadcastMessage("Teleporting to " + teleport.getPosition().toString());
        }
    }
}
