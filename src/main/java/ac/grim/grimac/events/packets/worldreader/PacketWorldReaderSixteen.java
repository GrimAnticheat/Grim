package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk.WrappedPacketOutMapChunk;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketWorldReaderSixteen extends PacketListenerAbstract {
    public static Method getByCombinedID;

    public PacketWorldReaderSixteen() {
        super(PacketEventPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            long time = System.nanoTime();

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                BaseChunk[] chunks = new SixteenChunk[16];

                byte[] chunkData = packet.getCompressedData();
                int availableSectionsInt = packet.getPrimaryBitMap().isPresent() ? packet.getPrimaryBitMap().get() : 0;
                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

                for (int index = 0; index < chunks.length; ++index) {
                    if ((availableSectionsInt & 1 << index) != 0) {
                        chunks[index] = SixteenChunk.read(dataIn);
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int combinedID = 0;

            // For 1.8 all the way to 1.16, the method for getting combined ID has never changed
            try {
                Object blockObject = wrappedBlockChange.readAnyObject(1);
                combinedID = (int) getByCombinedID.invoke(null, blockObject);
            } catch (InvocationTargetException | IllegalAccessException var4) {
                var4.printStackTrace();
            }

            Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

            player.sendTransaction();
            player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                // Section Position or Chunk Section - depending on version
                Object position = packet.readAnyObject(0);

                // In 1.16, chunk sections are used.  The have X, Y, and Z
                Method getX = Reflection.getMethod(position.getClass(), "getX", 0);
                Method getZ = Reflection.getMethod(position.getClass(), "getZ", 0);

                int chunkX = (int) getX.invoke(position) << 4;
                int chunkZ = (int) getZ.invoke(position) << 4;

                Method getY = Reflection.getMethod(position.getClass(), "getY", 0);
                int chunkY = (int) getY.invoke(position) << 4;

                short[] blockPositions = packet.readShortArray(0);
                Object[] blockDataArray = (Object[]) packet.readAnyObject(2);

                player.sendTransaction();
                for (int i = 0; i < blockPositions.length; i++) {
                    short blockPosition = blockPositions[i];

                    int blockX = sixteenSectionRelativeX(blockPosition);
                    int blockY = sixteenSectionRelativeY(blockPosition);
                    int blockZ = sixteenSectionRelativeZ(blockPosition);

                    int blockID = (int) getByCombinedID.invoke(null, blockDataArray[i]);

                    player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));
                }

            } catch (IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedWorld.removeChunk(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
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
