package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.seven.ByteArray3d;
import ac.grim.grimac.utils.chunkdata.seven.NibbleArray3d;
import ac.grim.grimac.utils.chunkdata.seven.SevenChunk;
import ac.grim.grimac.utils.chunks.Column;
import com.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import com.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import com.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import com.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import com.github.retrooper.packetevents.utils.nms.NMSUtils;
import com.github.retrooper.packetevents.utils.reflection.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class PacketWorldReaderSeven extends BasePacketWorldReader {
    public static Method ancientGetById;

    public PacketWorldReaderSeven() {
        ancientGetById = Reflection.getMethod(NMSUtils.blockClass, "getId", int.class);
    }

    @Override
    public void handleMapChunk(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());

        int chunkX = packet.getChunkX();
        int chunkZ = packet.getChunkZ();

        // Map chunk packet with 0 sections and continuous chunk is the unload packet in 1.7 and 1.8
        // Optional is only empty on 1.17 and above
        if (packet.readInt(5) == 0 && packet.isGroundUpContinuous().get()) {
            player.compensatedWorld.removeChunkLater(chunkX, chunkZ);
            return;
        }

        byte[] data = packet.getCompressedData();
        SevenChunk[] chunks = new SevenChunk[16];

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        readChunk(buf, chunks, packet.getBitSet().get());

        addChunkToCache(player, chunks, packet.isGroundUpContinuous().get(), chunkX, chunkZ);
    }

    @Override
    public void handleMapChunkBulk(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
        int[] chunkXArray = packet.readIntArray(0);
        int[] chunkZArray = packet.readIntArray(1);
        int[] bitset = packet.readIntArray(2);

        byte[][] byteArrayArray = packet.readObject(0, byte[][].class);

        for (int i = 0; i < chunkXArray.length; i++) {
            SevenChunk[] chunks = new SevenChunk[16];
            int chunkX = chunkXArray[i];
            int chunkZ = chunkZArray[i];

            ByteBuffer buf = ByteBuffer.wrap(byteArrayArray[i]).order(ByteOrder.LITTLE_ENDIAN);
            readChunk(buf, chunks, BitSet.valueOf(new long[]{bitset[i]}));

            Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
            player.compensatedWorld.addToCache(column, chunkX, chunkZ);
        }
    }

    public void readChunk(ByteBuffer buf, SevenChunk[] chunks, BitSet primarySet) {
        // 0 = Calculate expected length and determine if the packet has skylight.
        // 1 = Create chunks from mask and get blocks.
        // 2 = Get metadata.
        // 3 = Get block light.
        // 4 = Get sky light.
        // 5 = Get extended block data - This doesn't exist!
        //
        // Fun fact, a mojang dev (forgot who) wanted to do the flattening in 1.8
        // So the extended block data was likely how mojang wanted to get around the 255 block id limit
        // Before they decided to quite using magic values and instead went with the new 1.13 solution
        //
        // That's probably why extended block data exists, although yeah it was never used.
        //
        // (We only need blocks and metadata)
        for (int pass = 1; pass < 3; pass++) {
            for (int ind = 0; ind < 16; ind++) {
                if (primarySet.get(ind)) {
                    if (pass == 1) {
                        chunks[ind] = new SevenChunk();
                        ByteArray3d blocks = chunks[ind].getBlocks();
                        buf.get(blocks.getData(), 0, blocks.getData().length);
                    }

                    if (pass == 2) {
                        NibbleArray3d metadata = chunks[ind].getMetadata();
                        buf.get(metadata.getData(), 0, metadata.getData().length);
                    }
                }
            }
        }
    }

    @Override
    public void handleBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());

        try {
            // 1.7 includes the block data right in the packet
            Field id = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "data");
            int blockData = id.getInt(event.getNMSPacket().getRawNMSPacket());

            Field block = Reflection.getField(event.getNMSPacket().getRawNMSPacket().getClass(), "block");
            Object blockNMS = block.get(event.getNMSPacket().getRawNMSPacket());

            int materialID = (int) ancientGetById.invoke(null, blockNMS);
            int combinedID = materialID + (blockData << 12);

            handleUpdateBlockChange(player, event, wrappedBlockChange, combinedID);

        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacket packet = new WrappedPacket(event.getNMSPacket());

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
                event.setPostTask(player::sendTransaction);

            while (buffer.hasRemaining()) {
                short positionData = buffer.getShort();
                short block = buffer.getShort();

                int relativeX = positionData >> 12 & 15;
                int relativeZ = positionData >> 8 & 15;
                int relativeY = positionData & 255;

                int blockID = block >> 4 & 255;
                int blockMagicValue = block & 15;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.compensatedWorld.updateBlock(chunkX + relativeX, relativeY, chunkZ + relativeZ, blockID | blockMagicValue << 12));
            }
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            exception.printStackTrace();
        }
    }
}
