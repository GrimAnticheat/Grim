package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketPlayerTeleport extends PacketListenerAbstract {
    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {

        if (event.getPacketId() == PacketType.Play.Server.POSITION) {
            WrappedPacketOutPosition teleport = new WrappedPacketOutPosition(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            // Occurs on login
            if (player == null) return;

            byte relative = teleport.getRelativeFlagsMask();
            Vector3d pos = teleport.getPosition();
            float pitch = teleport.getPitch();
            float yaw = teleport.getYaw();

            // Convert relative teleports to normal teleports
            // We have to do this because 1.8 players on 1.9+ get teleports changed by ViaVersion
            // Additionally, velocity is kept after relative teleports making predictions difficult
            // The added complexity isn't worth a feature that I have never seen used
            if ((relative & 1) == 1)
                pos = pos.add(new Vector3d(player.packetStateData.packetPlayerX, 0, 0));

            if ((relative >> 1 & 1) == 1)
                pos = pos.add(new Vector3d(0, player.packetStateData.packetPlayerY, 0));

            if ((relative >> 2 & 1) == 1)
                pos = pos.add(new Vector3d(0, 0, player.packetStateData.packetPlayerZ));

            if ((relative >> 3 & 1) == 1)
                yaw += player.packetStateData.packetPlayerXRot;

            if ((relative >> 3 & 1) == 1)
                pitch += player.packetStateData.packetPlayerYRot;

            // Stop bad packets false by sending angles over 360
            yaw %= 360;
            pitch %= 360;

            teleport.setPosition(pos);
            teleport.setYaw(yaw);
            teleport.setPitch(pitch);
            teleport.setRelativeFlagsMask((byte) 0);

            player.teleports.add(pos);
        }
    }
}
