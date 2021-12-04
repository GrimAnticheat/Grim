package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketWorldReaderEighteen extends PacketWorldReaderSixteen {

    @Override
    public void handleMapChunk(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());

        try {
            int chunkX = packet.getChunkX();
            int chunkZ = packet.getChunkZ();

            byte[] chunkData = packet.getCompressedData();
            NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

            List<BaseChunk> temp = new ArrayList<>();

            while (dataIn.available() > 0) {
                try {
                    SixteenChunk chunk = SixteenChunk.read(dataIn);
                    temp.add(chunk);

                    // Skip past the biome data
                    int length = dataIn.readUnsignedByte();

                    // Simulate reading past the palette for biomes
                    if (length > 3) { // Writes nothing
                        // do nothing
                    } else if (length == 0) { // Writes the single member of the palette
                        dataIn.readVarInt(); // Read single member of palette
                    } else { // Writes size, then var ints for each size
                        int paletteLength = dataIn.readVarInt();
                        for (int i = 0; i < paletteLength; i++) {
                            dataIn.readVarInt();
                        }
                    }

                    dataIn.readLongs(dataIn.readVarInt());
                } catch (IOException e) { // Sometimes mojang sends extra byte data that isn't a chunk, unsure why... bug?
                    // TODO: Read the world size with packets instead of bukkit to avoid using exception to hack around mojang's bug
                    break;
                }
            }

            // Ground up was removed in 1.17
            BaseChunk[] chunks = new BaseChunk[temp.size()];
            addChunkToCache(player, temp.toArray(chunks), true, chunkX, chunkZ);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
