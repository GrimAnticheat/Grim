package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.TeleportData;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class BasePacketWorldReader extends PacketListenerAbstract {

    public BasePacketWorldReader() {
        super(PacketListenerPriority.HIGH);
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

        if (event.getPacketType() == PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerAcknowledgeBlockChanges changes = new WrapperPlayServerAcknowledgeBlockChanges(event);
            player.compensatedWorld.handlePredictionConfirmation(changes.getSequence());
        }

        if (event.getPacketType() == PacketType.Play.Server.ACKNOWLEDGE_PLAYER_DIGGING) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerAcknowledgePlayerDigging ack = new WrapperPlayServerAcknowledgePlayerDigging(event);
            player.compensatedWorld.handleBlockBreakAck(ack.getBlockPosition(), ack.getBlockId(), ack.getAction(), ack.isSuccessful());
        }

        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerChangeGameState newState = new WrapperPlayServerChangeGameState(event);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                if (newState.getReason() == WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING) {
                    player.compensatedWorld.isRaining = true;
                } else if (newState.getReason() == WrapperPlayServerChangeGameState.Reason.END_RAINING) {
                    player.compensatedWorld.isRaining = false;
                } else if (newState.getReason() == WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE) {
                    player.compensatedWorld.isRaining = newState.getValue() > 0.2f;
                }
            });
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
        addChunkToCache(event, player, chunkData.getColumn().getChunks(), chunkData.getColumn().isFullChunk(), chunkData.getColumn().getX(), chunkData.getColumn().getZ());
        event.setLastUsedWrapper(null);
    }

    public void addChunkToCache(PacketSendEvent event, GrimPlayer player, BaseChunk[] chunks, boolean isGroundUp, int chunkX, int chunkZ) {
        double chunkCenterX = (chunkX << 4) + 8;
        double chunkCenterZ = (chunkZ << 4) + 8;
        boolean shouldPostTrans = Math.abs(player.x - chunkCenterX) < 16 && Math.abs(player.z - chunkCenterZ) < 16;

        for (TeleportData teleports : player.getSetbackTeleportUtil().pendingTeleports) {
            if (teleports.getFlags().getMask() != 0) {
                continue; // Worse that will happen is people will get an extra setback...
            }
            shouldPostTrans = shouldPostTrans || (Math.abs(teleports.getLocation().getX() - chunkCenterX) < 16 && Math.abs(teleports.getLocation().getZ() - chunkCenterZ) < 16);
        }

        if (shouldPostTrans) {
            event.getTasksAfterSend().add(player::sendTransaction); // Player is in this unloaded chunk
        }
        if (isGroundUp) {
            Column column = new Column(chunkX, chunkZ, chunks, player.lastTransactionSent.get());
            player.compensatedWorld.addToCache(column, chunkX, chunkZ);
        } else {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                Column existingColumn = player.compensatedWorld.getChunk(chunkX, chunkZ);
                if (existingColumn == null) {
                    // Corrupting the player's empty chunk is actually quite meaningless
                    // You are able to set blocks inside it, and they do apply, it just always returns air despite what its data says
                    // So go ahead, corrupt the player's empty chunk and make it no longer all air, it doesn't matter
                    //
                    // LogUtil.warn("Invalid non-ground up continuous sent for empty chunk " + chunkX + " " + chunkZ + " for " + player.user.getProfile().getName() + "! This corrupts the player's empty chunk!");
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
        // Don't spam transactions (block changes are sent in batches)
        if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range &&
                player.lastTransSent + 2 < System.currentTimeMillis())
            player.sendTransaction();

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), blockChange.getBlockId()));
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);

        boolean didSend = false;
        int range = 16;

        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : multiBlockChange.getBlocks()) {
            // Don't send a transaction unless it's within 16 blocks of the player
            if (!didSend && Math.abs(blockChange.getX() - player.x) < range && Math.abs(blockChange.getY() - player.y) < range && Math.abs(blockChange.getZ() - player.z) < range &&
                    player.lastTransSent + 2 < System.currentTimeMillis()) {
                didSend = true;
                player.sendTransaction();
            }

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(blockChange.getX(), blockChange.getY(), blockChange.getZ(), blockChange.getBlockId()));
        }
    }
}