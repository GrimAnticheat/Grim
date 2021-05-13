package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.WorldChangeBlockData;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketWorldReader extends PacketListenerDynamic {
    public static Method getByCombinedID;

    public PacketWorldReader() throws ClassNotFoundException, NoSuchMethodException {
        super(PacketEventPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);
    }

    public static int sectionRelativeX(short data) {
        return data >>> 8 & 15;
    }

    public static int sectionRelativeY(short data) {
        return data & 15;
    }

    public static int sectionRelativeZ(short data) {
        return data >>> 4 & 15;
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
            WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                Object blockObject = wrappedBlockChange.readAnyObject(1);

                int blockID = (int) getByCombinedID.invoke(null, blockObject);
                Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

                player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), blockID));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            Object blockChange = event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                // Reflect to the chunk section position
                Field sectionField = blockChange.getClass().getDeclaredField("a");
                sectionField.setAccessible(true);

                // SectionPosition
                Object position = sectionField.get(blockChange);

                // Get the chunk section position itself
                Method getX = position.getClass().getMethod("a");
                Method getY = position.getClass().getMethod("b");
                Method getZ = position.getClass().getMethod("c");

                int chunkX = (int) getX.invoke(position) << 4;
                int chunkZ = (int) getZ.invoke(position) << 4;
                int chunkY = (int) getY.invoke(position) << 4;

                Field blockPositionsField = blockChange.getClass().getDeclaredField("b");
                blockPositionsField.setAccessible(true);

                Field blockDataField = blockChange.getClass().getDeclaredField("c");
                blockDataField.setAccessible(true);

                short[] blockPositions = (short[]) blockPositionsField.get(blockChange);
                Object[] blockDataArray = (Object[]) blockDataField.get(blockChange);

                for (int i = 0; i < blockPositions.length; i++) {
                    short blockPosition = blockPositions[i];

                    int blockX = sectionRelativeX(blockPosition);
                    int blockY = sectionRelativeY(blockPosition);
                    int blockZ = sectionRelativeZ(blockPosition);

                    int blockID = (int) getByCombinedID.invoke(null, blockDataArray[i]);

                    player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));

                }

            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            player.compensatedWorld.removeChunk(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }
    }
}
