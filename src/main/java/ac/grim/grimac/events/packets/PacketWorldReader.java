package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.WorldChangeBlockData;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockAction;
import net.minecraft.server.v1_16_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R3.SectionPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PacketWorldReader extends PacketListenerDynamic {
    public static Method getByCombinedID;
    public static Method getX;
    public static Method getY;
    public static Method getZ;

    public PacketWorldReader() throws ClassNotFoundException, NoSuchMethodException {
        super(PacketEventPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);
        getX = Reflection.getMethod(NMSUtils.blockPosClass, "getX", 0);
        getY = Reflection.getMethod(NMSUtils.blockPosClass, "getY", 0);
        getZ = Reflection.getMethod(NMSUtils.blockPosClass, "getZ", 0);
    }

    public static int sectionRelativeX(short data) {
        return data >>> 8 & 15;
    }

    public static int sectionRelativeY(short data) {
        return data & 15;
    }

    public static int sectionRelativeZ(short data) {
        return data >>> 4 & 15;
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (event.getPacketId() == PacketType.Play.Server.CHAT) return;
        Bukkit.broadcastMessage(event.getPacketName());

        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            // PacketPlayOutMapChunk
            Object chunk = event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                Field x = chunk.getClass().getDeclaredField("a");
                Field z = chunk.getClass().getDeclaredField("b");
                Field availableSections = chunk.getClass().getDeclaredField("c");
                Field buffer = chunk.getClass().getDeclaredField("f");

                x.setAccessible(true);
                z.setAccessible(true);
                availableSections.setAccessible(true);
                buffer.setAccessible(true);

                byte[] chunkData = (byte[]) buffer.get(chunk);
                int availableSectionsInt = availableSections.getInt(chunk);
                int chunkX = x.getInt(chunk);
                int chunkZ = z.getInt(chunk);

                NetInput dataIn = new StreamNetInput(new ByteArrayInputStream(chunkData));
                Chunk[] chunks = new Chunk[16];

                for (int index = 0; index < chunks.length; ++index) {
                    if ((availableSectionsInt & 1 << index) != 0) {
                        chunks[index] = Chunk.read(dataIn);
                    }
                }

                Column column = new Column(chunkX, chunkZ, chunks);
                player.compensatedWorld.addToCache(column, chunkX, chunkZ);

            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            // PacketPlayOutBlockChange
            Object blockChange = event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                Field position = blockChange.getClass().getDeclaredField("a");
                position.setAccessible(true);

                Field block = blockChange.getClass().getDeclaredField("block");
                block.setAccessible(true);

                // BlockPosition
                Object blockPosition = position.get(blockChange);

                int blockID = (int) getByCombinedID.invoke(null, block.get(blockChange));

                player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), (Integer) getX.invoke(blockPosition), (Integer) getY.invoke(blockPosition), (Integer) getZ.invoke(blockPosition), blockID));
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            PacketPlayOutMultiBlockChange blockChange = (PacketPlayOutMultiBlockChange) event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                // Reflect to the chunk section position
                Field sectionField = blockChange.getClass().getDeclaredField("a");
                sectionField.setAccessible(true);

                SectionPosition position = (SectionPosition) sectionField.get(blockChange);

                // Get the chunk section position itself
                Method getX = position.getClass().getMethod("a");
                Method getY = position.getClass().getMethod("b");
                Method getZ = position.getClass().getMethod("c");

                int chunkX = (int) getX.invoke(position) << 4;
                int chunkZ = (int) getZ.invoke(position) << 4;
                int chunkY = (int) getY.invoke(position) << 4;

                Field blockPositionsField = blockChange.getClass().getDeclaredField("b");
                blockPositionsField.setAccessible(true);

                Field blockDataField = blockChange.getClass().getDeclaredField("c");
                blockDataField.setAccessible(true);

                short[] blockPositions = (short[]) blockPositionsField.get(blockChange);
                Object[] blockDataArray = (Object[]) blockDataField.get(blockChange);

                for (int i = 0; i < blockPositions.length; i++) {
                    short blockPosition = blockPositions[i];

                    int blockX = sectionRelativeX(blockPosition);
                    int blockY = sectionRelativeY(blockPosition);
                    int blockZ = sectionRelativeZ(blockPosition);

                    int blockID = (int) getByCombinedID.invoke(null, blockDataArray[i]);

                    player.compensatedWorld.worldChangedBlockQueue.add(new WorldChangeBlockData(player.lastTransactionSent.get(), chunkX + blockX, chunkY + blockY, chunkZ + blockZ, blockID));

                }

            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            player.compensatedWorld.removeChunk(unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }

        if (packetID == PacketType.Play.Server.BLOCK_ACTION) {
            PacketPlayOutBlockAction action = (PacketPlayOutBlockAction) event.getNMSPacket().getRawNMSPacket();
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            try {
                Field blockPosition = action.getClass().getDeclaredField("a");

                BlockPosition position = (BlockPosition) blockPosition.get(action);
                int x = position.getX();
                int y = position.getY();
                int z = position.getZ();

                BlockData blockData = player.compensatedWorld.getBukkitBlockDataAt(x, y, z);

                if (blockData.getMaterial() == Material.PISTON || blockData.getMaterial() == Material.STICKY_PISTON) {
                    while (true) {
                        PistonData data = player.compensatedWorld.pistonData.peek();

                        if (data == null) break;

                        // The player hasn't gotten this update yet
                        if (data.lastTransactionSent > player.packetLastTransactionReceived) {
                            break;
                        }

                        player.compensatedWorld.pistonData.poll();
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

        }

    }
}
