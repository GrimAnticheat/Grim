package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_18;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class PacketWorldReaderEighteen extends BasePacketWorldReader {
    @Override
    public void handleMapChunk(GrimPlayer player, PacketSendEvent event) {
        PacketWrapper wrapper = new PacketWrapper(event);

        int x = wrapper.readInt();
        int z = wrapper.readInt();

        // Skip past heightmaps
        wrapper.readNBT();

        BaseChunk[] chunks = new ChunkReader_v1_18().read(null, null, true, false, false, (player.playerWorld.getMaxHeight() - player.playerWorld.getMinHeight()) >> 4, wrapper.readByteArray());

        addChunkToCache(player, chunks, true, x, z);

        event.setLastUsedWrapper(null);
    }
}
