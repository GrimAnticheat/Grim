package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.utils.chunks.ChunkCache;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PacketWorldReader extends PacketListenerDynamic {
    private static final int MIN_PALETTE_BITS_PER_ENTRY = 4;
    private static final int MAX_PALETTE_BITS_PER_ENTRY = 8;
    private static final int GLOBAL_PALETTE_BITS_PER_ENTRY = 14;
    public static Method blockCacheField;

    public PacketWorldReader() throws NoSuchFieldException {
        super(PacketEventPriority.MONITOR);

        // Yes, we are using reflection to get a reflected class. I'm not maintaining my own reflection.
        blockCacheField = Reflection.getMethod(NMSUtils.iBlockDataClass, "getBlock", 0);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            PacketPlayOutMapChunk chunk = (PacketPlayOutMapChunk) event.getNMSPacket().getRawNMSPacket();

            try {
                Field x = chunk.getClass().getDeclaredField("a");
                Field z = chunk.getClass().getDeclaredField("b");
                Field availableSections = chunk.getClass().getDeclaredField("c");
                //Field heightMaps = chunk.getClass().getDeclaredField("d");
                //Field biomes = chunk.getClass().getDeclaredField("e");
                Field buffer = chunk.getClass().getDeclaredField("f");
                Field blockEntitiesTags = chunk.getClass().getDeclaredField("g");
                //Field fullChunk = chunk.getClass().getDeclaredField("h");

                x.setAccessible(true);
                z.setAccessible(true);
                availableSections.setAccessible(true);
                buffer.setAccessible(true);
                blockEntitiesTags.setAccessible(true);

                Chunk actualChunk;
                byte[] chunkData = (byte[]) buffer.get(chunk);
                int availableSectionsInt = availableSections.getInt(chunk);
                int chunkX = x.getInt(chunk);
                int chunkZ = z.getInt(chunk);

                if (availableSectionsInt == 0) {
                    actualChunk = new Chunk();
                } else {
                    //Bukkit.broadcastMessage("Chunk is at " + x.get(chunk) + " " + z.get(chunk));
                    //Bukkit.broadcastMessage("Available sections is " + availableSections.get(chunk));
                    //Bukkit.broadcastMessage("Buffer size is " + ((byte[]) buffer.get(chunk)).length);

                    actualChunk = Chunk.read(new StreamNetInput(new ByteArrayInputStream(chunkData)));
                }

                ChunkCache.addToCache(actualChunk, chunkX, chunkZ);


            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            PacketPlayOutBlockChange blockChange = (PacketPlayOutBlockChange) event.getNMSPacket().getRawNMSPacket();
            try {
                Field position = blockChange.getClass().getDeclaredField("a");
                position.setAccessible(true);

                BlockPosition blockPosition = (BlockPosition) position.get(blockChange);
                int chunkX = blockPosition.getX() >> 4;
                int chunkZ = blockPosition.getZ() >> 4;
                int blockID = Block.getCombinedId(blockChange.block);

                ChunkCache.updateBlock(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), blockID);
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }
    }
}
