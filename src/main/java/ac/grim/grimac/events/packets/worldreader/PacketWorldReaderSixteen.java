package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.data.ChangeBlockData;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.BitSet;

public class PacketWorldReaderSixteen extends PacketWorldReaderNine {

    @Override
    public void handleMapChunk(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());

        try {
            int chunkX = packet.getChunkX();
            int chunkZ = packet.getChunkZ();
            BitSet bitSet = packet.getBitSet();

            BaseChunk[] chunks = new SixteenChunk[bitSet.size()];
            byte[] chunkData = packet.getCompressedData();

            NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

            for (int index = 0; index < chunks.length; ++index) {
                if (bitSet.get(index)) {
                    chunks[index] = SixteenChunk.read(dataIn);
                }
            }

            boolean isGroundUp = packet.isGroundUpContinuous().orElse(true);
            addChunkToCache(player, chunks, isGroundUp, chunkX, chunkZ);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacket packet = new WrappedPacket(event.getNMSPacket());

        try {
            // Section Position or Chunk Section - depending on version
            int positionPos = ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 1 : 0;
            Object position = packet.readAnyObject(positionPos);

            // In 1.16, chunk sections are used.  The have X, Y, and Z
            Method getX = Reflection.getMethod(position.getClass(), "getX", 0);
            Method getZ = Reflection.getMethod(position.getClass(), "getZ", 0);

            int chunkX = (int) getX.invoke(position) << 4;
            int chunkZ = (int) getZ.invoke(position) << 4;

            Method getY = Reflection.getMethod(position.getClass(), "getY", 0);
            int chunkY = (int) getY.invoke(position) << 4;

            short[] blockPositions = packet.readShortArray(0);

            int blockDataPos = ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 3 : 2;
            Object[] blockDataArray = (Object[]) packet.readAnyObject(blockDataPos);

            int range = (player.getTransactionPing() / 100) + 32;
            if (Math.abs(chunkX - player.x) < range && Math.abs(chunkY - player.y) < range && Math.abs(chunkZ - player.z) < range)
                event.setPostTask(player::sendTransaction);

            for (int i = 0; i < blockPositions.length; i++) {
                short blockPosition = blockPositions[i];

                int blockX = sixteenSectionRelativeX(blockPosition);
                int blockY = sixteenSectionRelativeY(blockPosition);
                int blockZ = sixteenSectionRelativeZ(blockPosition);

                int blockID = getByCombinedID(blockDataArray[i]);

                player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));
            }

        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
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
