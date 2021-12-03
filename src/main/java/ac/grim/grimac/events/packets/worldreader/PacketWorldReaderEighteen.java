package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.PaletteType;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PacketWorldReaderEighteen extends PacketWorldReaderSixteen {
    // This can be done through packets but that requires reading NBT... packetevents 2.0 stuff
    private static final int paletteSize;

    static {
        int elements = 0;

        try {
            Class<?> registry = NMSUtils.getNMClass("core.IRegistry");
            Object aR = registry.getDeclaredField("aR").get(null);
            elements = ((Set<?>) Reflection.getMethod(aR.getClass(),"d", 1).invoke(aR)).size();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        paletteSize = 32 - Integer.numberOfLeadingZeros(elements - 1);
    }

    @Override
    public void handleMapChunk(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());

        try {
            int chunkX = packet.getChunkX();
            int chunkZ = packet.getChunkZ();

            byte[] chunkData = packet.getCompressedData();
            NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

            List<BaseChunk> temp = new ArrayList<>();

            while (dataIn.available() > 6) {
                temp.add(SixteenChunk.read(dataIn));
                DataPalette.read(dataIn, PaletteType.BIOME, paletteSize);
            }

            // Ground up was removed in 1.17
            BaseChunk[] chunks = new BaseChunk[temp.size()];
            addChunkToCache(player, temp.toArray(chunks), false, chunkX, chunkZ);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
