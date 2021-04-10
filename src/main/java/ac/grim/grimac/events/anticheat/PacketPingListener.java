package ac.grim.grimac.events.anticheat;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.keepalive.WrappedPacketInKeepAlive;
import io.github.retrooper.packetevents.packetwrappers.play.out.keepalive.WrappedPacketOutKeepAlive;
import org.bukkit.Bukkit;

import java.util.HashMap;

public class PacketPingListener extends PacketListenerDynamic {
    static HashMap<Long, Long> keepaliveSendTime = new HashMap<>();

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.KEEP_ALIVE) {
            WrappedPacketInKeepAlive alive = new WrappedPacketInKeepAlive(event.getNMSPacket());
            Bukkit.broadcastMessage("Ping " + (keepaliveSendTime.get(alive.getId()) - System.nanoTime()));
            keepaliveSendTime.remove(alive.getId());
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.KEEP_ALIVE) {
            WrappedPacketOutKeepAlive alive = new WrappedPacketOutKeepAlive(event.getNMSPacket());
            keepaliveSendTime.put(alive.getId(), System.nanoTime());
        }
    }
}
