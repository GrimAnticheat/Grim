package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_18;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class PacketWorldReaderEighteen extends BasePacketWorldReader {

    private static final ChunkReader_v1_18 CHUNK_READER_V_1_18 = new ChunkReader_v1_18();
    private static final ServerVersion SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion();
    private static final boolean PRE_1_21_5 = SERVER_VERSION.isOlderThan(ServerVersion.V_1_21_5);
    private static final boolean READ_TILE_ENTITIES = SERVER_VERSION.isNewerThanOrEquals(ServerVersion.V_26_2);

    // Mojang decided to include lighting in this packet.  It's inefficient to read it, so we replace PacketEvents logic.
    @Override
    public void handleMapChunk(GrimPlayer player, PacketSendEvent event) {
        PacketWrapper<?> wrapper = new PacketWrapper<>(event);

        int x = wrapper.readInt();
        int z = wrapper.readInt();

        // Skip past heightmaps
        if (PRE_1_21_5)
            wrapper.readNBT();
        else
            wrapper.readMap(HeightmapType::read, PacketWrapper::readLongArray);

        // Use the new ChunkReader method that works with PacketWrapper directly
        BaseChunk[] chunks = CHUNK_READER_V_1_18.read(
                DimensionTypes.OVERWORLD, null, null, true, false, false,
                event.getUser().getTotalWorldHeight() >> 4,
                wrapper.readVarInt(), // Length of chunk data length (arrayLength) to pass to the new ChunkReader method
                wrapper
        );

        // Remove biomes to save memory
        for (int i = 0; i < chunks.length; i++) {
            Chunk_v1_18 chunk = (Chunk_v1_18) chunks[i];
            if (chunk != null) {
                // I know I'm passing null into @NotNull, but it shouldn't affect anything.
                chunks[i] = new Chunk_v1_18(chunk.getBlockCount(), chunk.getFluidCount(), chunk.getChunkData(), null);
            }
        }

        TileEntity[] tileEntities = null;
        if (READ_TILE_ENTITIES) {
            int tileEntityCount = wrapper.readVarInt();
            tileEntities = new TileEntity[tileEntityCount];
            for (int i = 0; i < tileEntityCount; i++) {
                tileEntities[i] = new TileEntity(wrapper.readByte(), wrapper.readShort(), wrapper.readVarInt(), wrapper.readNBT());
            }
        }

        addChunkToCache(event, player, chunks, true, x, z, tileEntities);

        event.setLastUsedWrapper(null); // Prevent PacketEvents from using this incomplete wrapper later
    }
}
