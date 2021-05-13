package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.FlatChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.FifteenChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.WorldChangeBlockData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
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

    public static int sixteenSectionRelativeX(short data) {
        return data >>> 8 & 15;
    }

    public static int sixteenSectionRelativeY(short data) {
        return data & 15;
    }

    public static int sixteenSectionRelativeZ(short data) {
        return data >>> 4 & 15;
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                byte[] chunkData = packet.readByteArray(0);
                int chunkX = packet.readInt(0);
                int chunkZ = packet.readInt(1);
                int availableSectionsInt = packet.readInt(2);

                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));
                FlatChunk[] chunks;
                if (XMaterial.getVersion() > 15) {
                    chunks = new SixteenChunk[16];
                    for (int index = 0; index < chunks.length; ++index) {
                        if ((availableSectionsInt & 1 << index) != 0) {
                            chunks[index] = SixteenChunk.read(dataIn);
                        }
                    }
                } else {
                    chunks = new FifteenChunk[16];
                    for (int index = 0; index < chunks.length; ++index) {
                        if ((availableSectionsInt & 1 << index) != 0) {
                            chunks[index] = FifteenChunk.read(dataIn);
                        }
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IOException e) {
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
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                // Section Position or Chunk Section - depending on version
                Object position = packet.readAnyObject(0);

                // In 1.16, chunk sections are used.  The have X, Y, and Z
                if (XMaterial.getVersion() > 15) {
                    Method getX = Reflection.getMethod(position.getClass(), "getX", 0);
                    Method getZ = Reflection.getMethod(position.getClass(), "getZ", 0);

                    int chunkX = (int) getX.invoke(position) << 4;
                    int chunkZ = (int) getZ.invoke(position) << 4;

                    Method getY = Reflection.getMethod(position.getClass(), "getY", 0);
                    int chunkY = (int) getY.invoke(position) << 4;

                    short[] blockPositions = packet.readShortArray(0);
                    Object[] blockDataArray = (Object[]) packet.readAnyObject(2);

                    for (int i = 0; i < blockPositions.length; i++) {
                        short blockPosition = blockPositions[i];

                        int blockX = sixteenSectionRelativeX(blockPosition);
                        int blockY = sixteenSectionRelativeY(blockPosition);
                        int blockZ = sixteenSectionRelativeZ(blockPosition);

                        int blockID = (int) getByCombinedID.invoke(null, blockDataArray[i]);

                        player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));

                    }
                } else if (XMaterial.isNewVersion()) {
                    Object[] blockInformation = (Object[]) packet.readAnyObject(1);

                    // This shouldn't be possible
                    if (blockInformation.length == 0) return;

                    Field getX = position.getClass().getDeclaredField("x");
                    Field getZ = position.getClass().getDeclaredField("z");

                    int chunkX = getX.getInt(position) << 4;
                    int chunkZ = getZ.getInt(position) << 4;

                    Field shortField = Reflection.getField(blockInformation[0].getClass(), 0);
                    Field blockDataField = Reflection.getField(blockInformation[0].getClass(), 1);

                    for (Object o : blockInformation) {
                        short pos = shortField.getShort(o);
                        int blockID = (int) getByCombinedID.invoke(null, blockDataField.get(o));

                        int blockX = pos >> 12 & 15;
                        int blockY = pos & 255;
                        int blockZ = pos >> 8 & 15;

                        player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, blockY, chunkZ + blockZ, blockID));
                    }
                }

            } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException exception) {
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
