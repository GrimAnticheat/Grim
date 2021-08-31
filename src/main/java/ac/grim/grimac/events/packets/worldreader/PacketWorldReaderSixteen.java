package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.sixteen.SixteenChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
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
        super(PacketListenerPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                BaseChunk[] chunks = new SixteenChunk[16];

                byte[] chunkData = packet.getCompressedData();
                int availableSectionsInt = packet.getPrimaryBitMask().isPresent() ? packet.getPrimaryBitMask().get() : 0;
                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

                for (int index = 0; index < chunks.length; ++index) {
                    if ((availableSectionsInt & 1 << index) != 0) {
                        chunks[index] = SixteenChunk.read(dataIn);
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (IOException e) {
                e.printStackTrace();
            }

            event.setPostTask(player::sendAndFlushTransactionOrPingPong);
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;
            if (player.compensatedWorld.isResync) return;

            int combinedID = 0;

            // For 1.8 all the way to 1.17, the method for getting combined ID has never changed
            try {
                Object blockObject = wrappedBlockChange.readAnyObject(1);
                combinedID = (int) getByCombinedID.invoke(null, blockObject);
            } catch (InvocationTargetException | IllegalAccessException var4) {
                var4.printStackTrace();
            }

            Vector3i blockPosition = wrappedBlockChange.getBlockPosition();


            int range = (player.getTransactionPing() / 100) + 16;
            if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
                event.setPostTask(player::sendAndFlushTransactionOrPingPong);

            player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
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

                int range = (player.getTransactionPing() / 100) + 32;
                if (Math.abs(chunkX - player.x) < range && Math.abs(chunkY - player.y) < range && Math.abs(chunkZ - player.z) < range)
                    event.setPostTask(player::sendAndFlushTransactionOrPingPong);


                for (int i = 0; i < blockPositions.length; i++) {
                    short blockPosition = blockPositions[i];

                    int blockX = sixteenSectionRelativeX(blockPosition);
                    int blockY = sixteenSectionRelativeY(blockPosition);
                    int blockZ = sixteenSectionRelativeZ(blockPosition);

                    int blockID = (int) getByCombinedID.invoke(null, blockDataArray[i]);

                    player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));
                }

            } catch (IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            player.compensatedWorld.removeChunkLater(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
            event.setPostTask(player::sendAndFlushTransactionOrPingPong);
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
