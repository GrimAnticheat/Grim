package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import org.bukkit.entity.Player;

public class PacketPlayerJoin extends PacketListenerDynamic {

    // We need to do this as some packets are sent before bukkit login event is fired
    // It's a race condition if we check for logins on the main thread
    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.LOGIN) {
            Player bukkitPlayer = event.getPlayer();
            GrimPlayer player = new GrimPlayer(bukkitPlayer);
            player.lastX = bukkitPlayer.getLocation().getX();
            player.lastY = bukkitPlayer.getLocation().getY();
            player.lastZ = bukkitPlayer.getLocation().getZ();
            player.lastXRot = bukkitPlayer.getLocation().getYaw();
            player.lastYRot = bukkitPlayer.getLocation().getPitch();
            player.x = bukkitPlayer.getLocation().getX();
            player.y = bukkitPlayer.getLocation().getY();
            player.z = bukkitPlayer.getLocation().getZ();
            player.xRot = bukkitPlayer.getLocation().getYaw();
            player.yRot = bukkitPlayer.getLocation().getPitch();

            GrimAC.playerGrimHashMap.put(event.getPlayer(), new GrimPlayer(event.getPlayer()));
        }
    }
}
