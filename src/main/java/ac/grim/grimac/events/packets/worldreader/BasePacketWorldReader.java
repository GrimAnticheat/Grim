package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.ChangeBlockData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BasePacketWorldReader extends PacketListenerAbstract {
    private static Method getByCombinedID;

    public BasePacketWorldReader() {
        super(PacketListenerPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            unloadChunk(player, unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }

        // 1.7 and 1.8 only
        if (packetID == PacketType.Play.Server.MAP_CHUNK_BULK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            handleMapChunkBulk(player, event);
        }

        if (packetID == PacketType.Play.Server.MAP_CHUNK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            handleMapChunk(player, event);
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            handleBlockChange(player, event);
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            handleMultiBlockChange(player, event);
        }
    }

    public void handleMapChunkBulk(GrimPlayer player, PacketPlaySendEvent event) {

    }

    public void handleMapChunk(GrimPlayer player, PacketPlaySendEvent event) {

    }

    public void addChunkToCache(GrimPlayer player, BaseChunk[] chunks, boolean isGroundUp, int chunkX, int chunkZ) {
        if (isGroundUp) {
            Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
            player.compensatedWorld.addToCache(column, chunkX, chunkZ);
        } else {
            player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get() + 1, () -> {
                Column existingColumn = player.compensatedWorld.getChunk(chunkX, chunkZ);
                if (existingColumn == null) {
                    LogUtil.warn("Invalid non-ground up continuous sent for empty chunk " + chunkX + " " + chunkZ + " for " + player.bukkitPlayer.getName() + "! This corrupts the player's empty chunk!");
                    return;
                }
                existingColumn.mergeChunks(chunks);
            });
        }
    }

    public void unloadChunk(GrimPlayer player, int x, int z) {
        if (player == null) return;
        player.compensatedWorld.removeChunkLater(x, z);
    }

    public void handleBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
        if (player.compensatedWorld.isResync) return;

        Object blockObject = wrappedBlockChange.readAnyObject(1);
        int combinedID = getByCombinedID(blockObject);

        handleUpdateBlockChange(player, event, wrappedBlockChange, combinedID);
    }

    public int getByCombinedID(Object object) {
        try {
            return (int) getByCombinedID.invoke(null, object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketPlaySendEvent event) {

    }

    public void handleUpdateBlockChange(GrimPlayer player, PacketPlaySendEvent event, WrappedPacketOutBlockChange wrappedBlockChange, int combinedID) {
        Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

        int range = (player.getTransactionPing() / 100) + 16;
        if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
            event.setPostTask(player::sendTransaction);

        player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
    }
}
