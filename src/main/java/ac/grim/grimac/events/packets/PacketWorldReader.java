package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketWorldReader extends PacketListenerDynamic {
    public static Method getByCombinedID;
    public static Method getX;
    public static Method getY;
    public static Method getZ;

    public PacketWorldReader() throws ClassNotFoundException, NoSuchMethodException {
        super(PacketEventPriority.MONITOR);

        // Yes, we are using reflection to get a reflected class. I'm not maintaining my own reflection.
        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);
        getByCombinedID.setAccessible(true);

        getX = Reflection.getMethod(NMSUtils.blockPosClass, "getX", 0);
        getY = Reflection.getMethod(NMSUtils.blockPosClass, "getY", 0);
        getZ = Reflection.getMethod(NMSUtils.blockPosClass, "getZ", 0);
    }


    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            // PacketPlayOutMapChunk
            Object chunk = event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

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
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            // PacketPlayOutBlockChange
            Object blockChange = event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                Field position = blockChange.getClass().getDeclaredField("a");
                position.setAccessible(true);

                Field block = blockChange.getClass().getDeclaredField("block");
                block.setAccessible(true);

                // BlockPosition
                Object blockPosition = position.get(blockChange);

                int blockID = (int) getByCombinedID.invoke(null, block.get(blockChange));

                player.compensatedWorld.updateBlock((Integer) getX.invoke(blockPosition), (Integer) getY.invoke(blockPosition), (Integer) getZ.invoke(blockPosition), blockID);
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }
    }
}
