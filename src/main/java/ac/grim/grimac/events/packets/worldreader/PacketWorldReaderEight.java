package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.eight.EightChunk;
import ac.grim.grimac.utils.chunkdata.eight.ShortArray3d;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.utils.reflection.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.BitSet;

public class PacketWorldReaderEight extends PacketWorldReaderSeven {
    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        // Time to dump chunk data for 1.8 - 0.02 ms
        // Time to dump chunk data for 1.7 - 0.04 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                // Map chunk packet with 0 sections and continuous chunk is the unload packet in 1.7 and 1.8
                // Optional is only empty on 1.17 and above
                Object chunkMap = packet.readAnyObject(2);
                if (chunkMap.getClass().getDeclaredField("b").getInt(chunkMap) == 0 && packet.isGroundUpContinuous().get()) {
                    unloadChunk(player, chunkX, chunkZ);
                    return;
                }

                ShortBuffer buf = ByteBuffer.wrap(packet.getCompressedData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                EightChunk[] chunks = new EightChunk[16];
                BitSet set = packet.getBitSet();

                readChunk(buf, chunks, set);

                Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // Exists on 1.7 and 1.8 only
        if (packetID == PacketType.Play.Server.MAP_CHUNK_BULK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            int[] chunkXArray = (int[]) packet.readAnyObject(0);
            int[] chunkZArray = (int[]) packet.readAnyObject(1);
            Object[] chunkData = (Object[]) packet.readAnyObject(2);

            for (int i = 0; i < chunkXArray.length; i++) {
                EightChunk[] chunks = new EightChunk[16];
                int chunkX = chunkXArray[i];
                int chunkZ = chunkZArray[i];

                WrappedPacket nmsChunkMapWrapper = new WrappedPacket(new NMSPacket(chunkData[i]));
                ShortBuffer buf = ByteBuffer.wrap(nmsChunkMapWrapper.readByteArray(0)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

                readChunk(buf, chunks, BitSet.valueOf(new long[]{nmsChunkMapWrapper.readInt(0)}));

                Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);
            }
        }
    }

    public void readChunk(ShortBuffer buf, BaseChunk[] chunks, BitSet set) {
        int pos = 0;

        // We only need block data!
        // One pass is enough for us, no calculations required.
        //
        // Originally written abusing bukkit API with reflection... but this is faster, easier, and safer
        for (int ind = 0; ind < 16; ind++) {
            if (set.get(ind)) {
                ShortArray3d blocks = new ShortArray3d(4096);
                buf.position(pos / 2);
                buf.get(blocks.getData(), 0, blocks.getData().length);
                pos += blocks.getData().length * 2;

                chunks[ind] = new EightChunk(blocks);
            }
        }
    }

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
        if (player == null) return;

        try {
            // Section Position or Chunk Section - depending on version
            Object position = packet.readAnyObject(0);

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

            int range = (player.getTransactionPing() / 100) + 32;
            if (Math.abs(chunkX - player.x) < range && Math.abs(chunkZ - player.z) < range)
                event.setPostTask(player::sendTransaction);


            for (Object o : blockInformation) {
                short pos = shortField.getShort(o);
                int blockID = (int) getByCombinedID.invoke(null, blockDataField.get(o));

                int blockX = pos >> 12 & 15;
                int blockY = pos & 255;
                int blockZ = pos >> 8 & 15;

                player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, chunkX + blockX, blockY, chunkZ + blockZ, blockID));
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException exception) {
            exception.printStackTrace();
        }
    }
}
