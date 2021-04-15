package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.transaction.WrappedPacketInTransaction;
import io.github.retrooper.packetevents.packetwrappers.play.out.keepalive.WrappedPacketOutKeepAlive;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PacketPingListener extends PacketListenerDynamic {
    static HashMap<Long, Long> keepaliveSendTime = new HashMap<>();
    static HashMap<Player, Long> grimacSendTime = new HashMap<>();

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.TRANSACTION) {
            WrappedPacketInTransaction transaction = new WrappedPacketInTransaction(event.getNMSPacket());
            short id = transaction.getActionNumber();

            // Vanilla always uses an ID starting from 1
            if (id < 0) {
                GrimAC.playerGrimHashMap.get(event.getPlayer()).addTransactionResponse(id);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.KEEP_ALIVE) {
            WrappedPacketOutKeepAlive alive = new WrappedPacketOutKeepAlive(event.getNMSPacket());
            keepaliveSendTime.put(alive.getId(), System.nanoTime());

            if (alive.getId() == 64656669) {
                grimacSendTime.put(event.getPlayer(), System.nanoTime());
            }
        }
    }
}
