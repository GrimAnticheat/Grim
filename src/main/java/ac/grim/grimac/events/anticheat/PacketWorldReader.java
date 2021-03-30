package ac.grim.grimac.events.anticheat;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;

public class PacketWorldReader extends PacketListenerDynamic {
    public PacketWorldReader() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            PacketPlayOutMapChunk chunk = (PacketPlayOutMapChunk) event.getNMSPacket().getRawNMSPacket();
            try {
                Field x = chunk.getClass().getDeclaredField("a");
                Field z = chunk.getClass().getDeclaredField("b");
                x.setAccessible(true);
                z.setAccessible(true);

                Bukkit.broadcastMessage("Sent chunk with coords " + x.getInt(chunk) + " and " + z.getInt(chunk));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
