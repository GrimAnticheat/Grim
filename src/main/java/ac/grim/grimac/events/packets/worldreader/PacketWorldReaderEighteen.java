package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.stream.NetStreamInput;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_18;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.io.ByteArrayInputStream;

public class PacketWorldReaderEighteen extends BasePacketWorldReader {
    @Override
    public void handleMapChunk(GrimPlayer player, PacketSendEvent event) {
        PacketWrapper wrapper = new PacketWrapper(event);

        int x = wrapper.readInt();
        int z = wrapper.readInt();

        // Skip past heightmaps
        wrapper.readNBT();

        BaseChunk[] chunks = new ChunkReader_v1_18().read(null, null, true, false, false, (player.compensatedWorld.clientboundMaxHeight - player.compensatedWorld.clientboundMinHeight) >> 4, null, new NetStreamInput(new ByteArrayInputStream(wrapper.readByteArray())));

        addChunkToCache(player, chunks, true, x, z);

        event.setLastUsedWrapper(null);
    }
}
