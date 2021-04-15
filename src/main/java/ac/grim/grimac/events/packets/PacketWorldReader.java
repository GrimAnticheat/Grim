package ac.grim.grimac.events.packets;

import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.chunks.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
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

    //private static final String NMS_VERSION_SUFFIX = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName()
    //        .replace(".", ",").split(",")[3] + ".";

    public PacketWorldReader() throws ClassNotFoundException, NoSuchMethodException {
        super(PacketEventPriority.MONITOR);

        // Yes, we are using reflection to get a reflected class. I'm not maintaining my own reflection.
        //blockCacheField = Reflection.getMethod(NMSUtils.iBlockDataClass, "getBlock", 0);

        //Block.getByCombinedId();
        //blockCacheField.setAccessible(true);
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
                Field buffer = chunk.getClass().getDeclaredField("f");

                x.setAccessible(true);
                z.setAccessible(true);
                availableSections.setAccessible(true);
                buffer.setAccessible(true);

                byte[] chunkData = (byte[]) buffer.get(chunk);
                int availableSectionsInt = availableSections.getInt(chunk);
                int chunkX = x.getInt(chunk);
                int chunkZ = z.getInt(chunk);

                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));
                Chunk[] chunks = new Chunk[16];

                for (int index = 0; index < chunks.length; ++index) {
                    if ((availableSectionsInt & 1 << index) != 0) {
                        chunks[index] = Chunk.read(dataIn);
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                ChunkCache.addToCache(column, chunkX, chunkZ);

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
                int blockID = Block.getCombinedId(blockChange.block);

                ChunkCache.updateBlock(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), blockID);
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }
    }
}
