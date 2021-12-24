package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.chunks.Column;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.entity.Player;

public class BasePacketWorldReader extends PacketListenerAbstract {

    public BasePacketWorldReader() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk unloadChunk = new WrapperPlayServerUnloadChunk(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            unloadChunk(player, unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }

        // 1.7 and 1.8 only
        if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK_BULK) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            handleMapChunkBulk(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            handleMapChunk(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            handleBlockChange(player, event);
        }

        if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            handleMultiBlockChange(player, event);
        }
    }

    public void handleMapChunkBulk(GrimPlayer player, PacketSendEvent event) {
        // Only exists in 1.7 and 1.8
    }

    public void handleMapChunk(GrimPlayer player, PacketSendEvent event) {
        throw new NotImplementedException();
    }

    public void addChunkToCache(GrimPlayer player, BaseChunk[] chunks, boolean isGroundUp, int chunkX, int chunkZ) {
        if (isGroundUp) {
            Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get() + 1);
            player.compensatedWorld.addToCache(column, chunkX, chunkZ);
        } else {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
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

    public void handleBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(event);
        handleUpdateBlockChange(player, event, blockChange.getBlockPosition(), blockChange.getBlockId());
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);
        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : multiBlockChange.getBlocks()) {
            handleUpdateBlockChange(player, event,
                    new Vector3i(blockChange.getX(), blockChange.getY(), blockChange.getZ()),
                    blockChange.getBlockID());
        }
    }

    public void handleUpdateBlockChange(GrimPlayer player, PacketSendEvent event, Vector3i blockPosition, int combinedID) {
        int range = (player.getTransactionPing() / 100) + 16;
        if (player.sendTrans && Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
            event.setPostTask(player::sendTransaction);

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.compensatedWorld.updateBlock(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
    }
}
