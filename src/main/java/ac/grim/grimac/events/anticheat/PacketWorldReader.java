package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.utils.chunks.ChunkCache;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;
import org.bukkit.Bukkit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class PacketWorldReader extends PacketListenerDynamic {
    private static final ChunkCache cache = new ChunkCache();

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
                //Field availableSections = chunk.getClass().getDeclaredField("c");
                Field buffer = chunk.getClass().getDeclaredField("f");
                /*Field heightMaps = chunk.getClass().getField("d");
                Field biomes = chunk.getClass().getField("e");

                Field blockEntitiesTags = chunk.getClass().getField("g");
                Field fullChunk = chunk.getClass().getField("e");*/

                x.setAccessible(true);
                z.setAccessible(true);
                buffer.setAccessible(true);
                //availableSections.setAccessible(true);
                /*heightMaps.setAccessible(true);
                biomes.setAccessible(true);
                buffer.setAccessible(true);
                blockEntitiesTags.setAccessible(true);
                fullChunk.setAccessible(true);*/

                byte[] chunkData = (byte[]) buffer.get(chunk);

                Chunk actualChunk = Chunk.read(new StreamNetInput(new ByteArrayInputStream(chunkData)));

                Bukkit.broadcastMessage("Block at (0,1,0) is " + actualChunk.get(0, 1, 0));
                Bukkit.broadcastMessage("Block at (0,2,0) is " + actualChunk.get(0, 2, 0));
                Bukkit.broadcastMessage("Block at (0,3,0) is " + actualChunk.get(0, 3, 0));
                Bukkit.broadcastMessage("Block at (0,4,0) is " + actualChunk.get(0, 4, 0));

            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
