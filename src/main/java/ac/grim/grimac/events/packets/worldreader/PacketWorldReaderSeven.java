package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.seven.SevenChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class PacketWorldReaderSeven extends PacketListenerAbstract {
    public static Method ancientGetById;

    public PacketWorldReaderSeven() {
        super(PacketListenerPriority.MONITOR);

        ancientGetById = Reflection.getMethod(NMSUtils.blockClass, "getId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        // Time to dump chunk data for 1.8 - 0.02 ms
        // Time to dump chunk data for 1.7 - 0.04 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                BaseChunk[] chunks = new SevenChunk[16];

                // Map chunk packet with 0 sections and continuous chunk is the unload packet in 1.7 and 1.8
                // Optional is only empty on 1.17 and above
                if (packet.readInt(5) == 0 && packet.isGroundUpContinuous().get()) {
                    player.compensatedWorld.removeChunk(chunkX, chunkZ);
                    return;
                }

                if (player.bukkitPlayer.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    Chunk sentChunk = player.bukkitPlayer.getWorld().getChunkAt(chunkX, chunkZ);
                    ChunkSnapshot snapshot = sentChunk.getChunkSnapshot();

                    int highestBlock = 0;

                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            highestBlock = Math.max(highestBlock, snapshot.getHighestBlockYAt(x, z));
                        }
                    }

                    // 1.7 chunk section logic is complicated and custom forks make it worse
                    // Just use the bukkit API as it copies all the data we need into an array
                    Field ids = Reflection.getField(snapshot.getClass(), "blockids");
                    Field data = Reflection.getField(snapshot.getClass(), "blockdata");

                    short[][] blockids = (short[][]) ids.get(snapshot);
                    byte[][] blockdata = (byte[][]) data.get(snapshot);

                    for (int x = 0; x < 16; x++) {
                        if (!snapshot.isSectionEmpty(x)) {
                            chunks[x] = new SevenChunk(blockids[x], blockdata[x]);
                        }
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // Exists on 1.7 and 1.8 only
        // Purposefully left this
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

                    BaseChunk[] chunks = new SevenChunk[16];

                    if (player.bukkitPlayer.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                        Chunk sentChunk = player.bukkitPlayer.getWorld().getChunkAt(chunkX, chunkZ);
                        ChunkSnapshot snapshot = sentChunk.getChunkSnapshot();

                        int highestBlock = 0;

                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                highestBlock = Math.max(highestBlock, snapshot.getHighestBlockYAt(x, z));
                            }
                        }

                        // 1.7 chunk section logic is complicated and custom forks make it worse
                        // Just use the bukkit API as it copies all the data we need into an array
                        Field ids = Reflection.getField(snapshot.getClass(), "blockids");
                        Field data = Reflection.getField(snapshot.getClass(), "blockdata");

                        short[][] blockids = (short[][]) ids.get(snapshot);
                        byte[][] blockdata = (byte[][]) data.get(snapshot);

                        for (int x = 0; x < 16; x++) {
                            if (!snapshot.isSectionEmpty(x)) {
                                chunks[x] = new SevenChunk(blockids[x], blockdata[x]);
                            }
                        }
                    }

                    Column column = new Column(chunkX, chunkZ, chunks);
                    player.compensatedWorld.addToCache(column, chunkX, chunkZ);

                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                // 1.7 includes the block data right in the packet
                Field id = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "data");
                int blockData = id.getInt(event.getNMSPacket().getRawNMSPacket());

                Field block = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "block");
                Object blockNMS = block.get(event.getNMSPacket().getRawNMSPacket());

                int materialID = (int) ancientGetById.invoke(null, blockNMS);
                int combinedID = materialID + (blockData << 12);

                Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

                int range = (player.getTransactionPing() / 100) + 16;
                if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
                    event.setPostTask(player::sendTransactionOrPingPong);

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

                int range = (player.getTransactionPing() / 100) + 32;
                if (Math.abs(chunkX - player.x) < range && Math.abs(chunkZ - player.z) < range)
                    event.setPostTask(player::sendTransactionOrPingPong);

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
            } catch (IllegalAccessException | NoSuchFieldException exception) {
                exception.printStackTrace();
            }
        }
    }
}
