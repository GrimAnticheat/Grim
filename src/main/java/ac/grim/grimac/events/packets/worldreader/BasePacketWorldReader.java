package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class BasePacketWorldReader extends PacketListenerAbstract {

    public BasePacketWorldReader() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk unloadChunk = new WrapperPlayServerUnloadChunk(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            unloadChunk(player, unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }

        // 1.7 and 1.8 only
        if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK_BULK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            handleMapChunkBulk(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            handleMapChunk(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            handleBlockChange(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            handleMultiBlockChange(player, event);
        }
    }

    public void handleMapChunkBulk(GrimPlayer player, PacketSendEvent event) {
        // Only exists in 1.7 and 1.8
        WrapperPlayServerChunkDataBulk chunkData = new WrapperPlayServerChunkDataBulk(event);
        for (int i = 0; i < chunkData.getChunks().length; i++) {
            addChunkToCache(event, player, chunkData.getChunks()[i], true, chunkData.getX()[i], chunkData.getZ()[i]);
        }
    }

    public void handleMapChunk(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(event);
        addChunkToCache(event, player, chunkData.getColumn().getChunks(), true, chunkData.getColumn().getX(), chunkData.getColumn().getZ());
        event.setLastUsedWrapper(null);
    }

    public void addChunkToCache(PacketSendEvent event, GrimPlayer player, BaseChunk[] chunks, boolean isGroundUp, int chunkX, int chunkZ) {
        if (GrimMath.floor(player.x) >> 4 == chunkX && GrimMath.floor(player.z) >> 4 == chunkZ) {
            event.getPostTasks().add(player::sendTransaction); // Player is in this unloaded chunk
        }
        if (isGroundUp) {
            Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
            player.compensatedWorld.addToCache(column, chunkX, chunkZ);
        } else {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                Column existingColumn = player.compensatedWorld.getChunk(chunkX, chunkZ);
                if (existingColumn == null) {
                    LogUtil.warn("Invalid non-ground up continuous sent for empty chunk " + chunkX + " " + chunkZ + " for " + player.user.getProfile().getName() + "! This corrupts the player's empty chunk!");
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

    public void handleBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(event);
        int range = 16;

        Vector3i blockPosition = blockChange.getBlockPosition();
        if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
            player.sendTransaction();

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), blockChange.getBlockId()));
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);
        player.sendTransaction();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : multiBlockChange.getBlocks()) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(blockChange.getX(), blockChange.getY(), blockChange.getZ(), blockChange.getBlockID()));
        }
    }
}
