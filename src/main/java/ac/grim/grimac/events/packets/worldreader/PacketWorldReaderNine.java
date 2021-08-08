package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.twelve.TwelveChunk;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketWorldReaderNine extends PacketListenerAbstract {
    public static Method getByCombinedID;
    public static Method ancientGetById;

    public PacketWorldReaderNine() {
        super(PacketListenerPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
        ancientGetById = Reflection.getMethod(NMSUtils.blockClass, "getId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        // Time to dump chunk data for 1.9+ - 0.07 ms
        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            WrappedPacketOutMapChunk packet = new WrappedPacketOutMapChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
                int chunkX = packet.getChunkX();
                int chunkZ = packet.getChunkZ();

                byte[] chunkData = packet.getCompressedData();
                int availableSectionsInt = packet.getPrimaryBitMask().isPresent() ? packet.getPrimaryBitMask().get() : 0;
                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));

                BaseChunk[] chunks = new TwelveChunk[16];
                for (int index = 0; index < chunks.length; ++index) {
                    if ((availableSectionsInt & 1 << index) != 0) {
                        chunks[index] = new TwelveChunk(dataIn);

                        // Advance the data past the blocklight and skylight bytes
                        dataIn.readBytes(4096);
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
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

            int range = (player.getTransactionPing() / 100) + 16;
            if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
                event.setPostTask(player::sendAndFlushTransactionOrPingPong);

            player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            try {
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
                    event.setPostTask(player::sendAndFlushTransactionOrPingPong);


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

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedWorld.removeChunkLater(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
            event.setPostTask(player::sendAndFlushTransactionOrPingPong);
        }
    }
}
