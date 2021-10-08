package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.FifteenChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.BitSet;

public class PacketWorldReaderThirteen extends PacketWorldReaderNine {
    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        super.onPacketPlaySend(event);

        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                BaseChunk[] chunks;
                byte[] chunkData = packet.getCompressedData();
                BitSet bitSet = packet.getBitSet();
                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

                chunks = new FifteenChunk[16];
                for (int index = 0; index < chunks.length; ++index) {
                    if (bitSet.get(index)) {
                        chunks[index] = FifteenChunk.read(dataIn);

                        // Advance the data past the blocklight and skylight bytes
                        if (XMaterial.getVersion() == 13) {
                            dataIn.readBytes(4096);
                        }
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
