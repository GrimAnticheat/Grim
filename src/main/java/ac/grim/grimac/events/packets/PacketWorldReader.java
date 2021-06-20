package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.eight.EightChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.FifteenChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunkdata.twelve.TwelveChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class PacketWorldReader extends PacketListenerAbstract {
    public static Method getByCombinedID;
    public static Method ancientGetById;

    public PacketWorldReader() throws ClassNotFoundException, NoSuchMethodException {
        super(PacketEventPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
        ancientGetById = Reflection.getMethod(NMSUtils.blockClass, "getId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        // Time to dump chunk data for 1.7/1.8 - 0.02 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                BaseChunk[] chunks;
                if (XMaterial.getVersion() > 8) {
                    byte[] chunkData = packet.getCompressedData();
                    int availableSectionsInt = packet.getPrimaryBitMap().isPresent() ? packet.getPrimaryBitMap().get() : 0;
                    NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

                    if (XMaterial.getVersion() > 15) {
                        chunks = new SixteenChunk[16];
                        for (int index = 0; index < chunks.length; ++index) {
                            if ((availableSectionsInt & 1 << index) != 0) {
                                chunks[index] = SixteenChunk.read(dataIn);
                            }
                        }
                    } else if (XMaterial.isNewVersion()) {
                        chunks = new FifteenChunk[16];
                        for (int index = 0; index < chunks.length; ++index) {
                            if ((availableSectionsInt & 1 << index) != 0) {
                                chunks[index] = FifteenChunk.read(dataIn);

                                // Advance the data past the blocklight and skylight bytes
                                if (XMaterial.getVersion() == 13) {
                                    dataIn.readBytes(4096);
                                }
                            }
                        }
                    } else {
                        chunks = new TwelveChunk[16];
                        for (int index = 0; index < chunks.length; ++index) {
                            if ((availableSectionsInt & 1 << index) != 0) {
                                chunks[index] = new TwelveChunk(dataIn);

                                // Advance the data past the blocklight and skylight bytes
                                dataIn.readBytes(4096);
                            }
                        }
                    }
                } else {
                    // Map chunk packet with 0 sections and continuous chunk is the unload packet in 1.7 and 1.8
                    // Optional is only empty on 1.17 and above
                    if (XMaterial.getVersion() == 8) {
                        Object chunkMap = packet.readAnyObject(2);
                        if (chunkMap.getClass().getDeclaredField("b").getInt(chunkMap) == 0 && packet.isGroundUpContinuous().get()) {
                            player.compensatedWorld.removeChunk(chunkX, chunkZ);
                            return;
                        }
                    } else {
                        if (packet.readInt(5) == 0 && packet.isGroundUpContinuous().get()) {
                            player.compensatedWorld.removeChunk(chunkX, chunkZ);
                            return;
                        }
                    }

                    // This isn't really async safe, but I've seen much worse on 1.7/1.8
                    chunks = new EightChunk[16];
                    if (player.bukkitPlayer.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                        Chunk sentChunk = player.bukkitPlayer.getWorld().getChunkAt(chunkX, chunkZ);

                        Method handle = Reflection.getMethod(sentChunk.getClass(), "getHandle", 0);
                        Object nmsChunk = handle.invoke(sentChunk);
                        Method sections = Reflection.getMethod(nmsChunk.getClass(), "getSections", 0);
                        Object sectionsArray = sections.invoke(nmsChunk);

                        int arrayLength = Array.getLength(sectionsArray);

                        if (arrayLength == 0)
                            return;

                        Method getIds = Reflection.getMethod(Array.get(sectionsArray, 0).getClass(), "getIdArray", 0);

                        for (int x = 0; x < arrayLength; x++) {
                            Object section = Array.get(sectionsArray, x);

                            if (section == null) break;

                            chunks[x] = new EightChunk((char[]) getIds.invoke(section));
                        }
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IOException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        // Exists on 1.7 and 1.8 only
        if (packetID == PacketType.Play.Server.MAP_CHUNK_BULK) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
                int[] chunkXArray = (int[]) packet.readAnyObject(0);
                int[] chunkZArray = (int[]) packet.readAnyObject(1);

                for (int i = 0; i < chunkXArray.length; i++) {
                    int chunkX = chunkXArray[i];
                    int chunkZ = chunkZArray[i];
                    EightChunk[] chunks = new EightChunk[16];

                    if (player.bukkitPlayer.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                        Chunk sentChunk = player.bukkitPlayer.getWorld().getChunkAt(chunkX, chunkZ);

                        Method handle = Reflection.getMethod(sentChunk.getClass(), "getHandle", 0);
                        Object nmsChunk = handle.invoke(sentChunk);
                        Method sections = Reflection.getMethod(nmsChunk.getClass(), "getSections", 0);
                        Object sectionsArray = sections.invoke(nmsChunk);

                        int arrayLength = Array.getLength(sectionsArray);

                        if (arrayLength == 0)
                            return;

                        Method getIds = Reflection.getMethod(Array.get(sectionsArray, 0).getClass(), "getIdArray", 0);

                        for (int x = 0; x < arrayLength; x++) {
                            Object section = Array.get(sectionsArray, x);

                            if (section == null) break;

                            chunks[x] = new EightChunk((char[]) getIds.invoke(section));
                        }

                        Column column = new Column(chunkX, chunkZ, chunks);
                        player.compensatedWorld.addToCache(column, chunkX, chunkZ);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                int combinedID = 0;

                if (XMaterial.getVersion() > 7) {
                    // For 1.8 all the way to 1.16, the method for getting combined ID has never changed
                    try {
                        Object blockObject = wrappedBlockChange.readAnyObject(1);
                        combinedID = (int) getByCombinedID.invoke(null, blockObject);
                    } catch (InvocationTargetException | IllegalAccessException var4) {
                        var4.printStackTrace();
                    }
                } else {
                    // 1.7 includes the block data right in the packet
                    Field id = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "data");
                    int blockData = id.getInt(event.getNMSPacket().getRawNMSPacket());

                    Field block = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "block");
                    Object blockNMS = block.get(event.getNMSPacket().getRawNMSPacket());

                    int materialID = (int) ancientGetById.invoke(null, blockNMS);

                    combinedID = materialID + (blockData << 12);
                }

                Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

                player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));

            } catch (IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                // Section Position or Chunk Section - depending on version
                Object position = packet.readAnyObject(XMaterial.getVersion() == 7 ? 1 : 0);

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

                        player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));

                    }
                } else if (XMaterial.getVersion() > 7) {
                    Object[] blockInformation;
                    blockInformation = (Object[]) packet.readAnyObject(1);

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

                        player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, blockY, chunkZ + blockZ, blockID));
                    }
                } else {
                    // 1.7 multi block change format:
                    // https://wiki.vg/index.php?title=Protocol&oldid=6003#Chunk_Data
                    // Object 1 - ChunkCoordIntPair
                    // Object 5 - Blocks array using integers
                    // 00 00 00 0F - block metadata
                    // 00 00 FF F0 - block ID
                    // 00 FF 00 00 - Y coordinate
                    // 0F 00 00 00 - Z coordinate relative to chunk
                    // F0 00 00 00 - X coordinate relative to chunk
                    Object coordinates = packet.readAnyObject(1);
                    int chunkX = coordinates.getClass().getDeclaredField("x").getInt(coordinates) << 4;
                    int chunkZ = coordinates.getClass().getDeclaredField("z").getInt(coordinates) << 4;

                    byte[] blockData = (byte[]) packet.readAnyObject(2);

                    ByteBuffer buffer = ByteBuffer.wrap(blockData);

                    while (buffer.hasRemaining()) {
                        short positionData = buffer.getShort();
                        short block = buffer.getShort();

                        int relativeX = positionData >> 12 & 15;
                        int relativeZ = positionData >> 8 & 15;
                        int relativeY = positionData & 255;

                        int blockID = block >> 4 & 255;
                        int blockMagicValue = block & 15;

                        player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), chunkX + relativeX, relativeY, chunkZ + relativeZ, blockID | blockMagicValue << 12));
                    }
                }

            } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedWorld.removeChunk(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }
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
}
