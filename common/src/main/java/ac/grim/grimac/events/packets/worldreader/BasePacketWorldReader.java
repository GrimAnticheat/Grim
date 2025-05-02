package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.TeleportData;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgePlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkDataBulk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import io.netty.buffer.ByteBuf;

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

        int x = blockPosition.getX();
        int y = blockPosition.getY();
        int z = blockPosition.getZ();
        int blockId = blockChange.getBlockId();

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(x, y, z, blockId));
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);

        int range = 16;

        final var blocks = multiBlockChange.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : blocks) {
            // Don't send a transaction unless it's within 16 blocks of the player
            if (Math.abs(blockChange.getX() - player.x) < range && Math.abs(blockChange.getY() - player.y) < range && Math.abs(blockChange.getZ() - player.z) < range && player.lastTransSent + 2 < System.currentTimeMillis()) {
                player.sendTransaction();
                break;
            }
        }

        // Add a single runnable to prevent excessive memory use when there are lots of block changes
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : blocks) {
                player.compensatedWorld.updateBlock(blockChange.getX(), blockChange.getY(), blockChange.getZ(), blockChange.getBlockId());
            }
        });
    }

    public void handleMultiBlockChangeFast(GrimPlayer player, PacketSendEvent event) {

        final Object byteBuf = event.getByteBuf();
        final int range     = 16;
        final long nowMillis = System.currentTimeMillis();

        /* ------------------------------------------------------------------
         * 1. ---  Decode the "section position" header ----------------------
         * ------------------------------------------------------------------ */
        final int  protocol    = player.getClientVersion().getProtocolVersion(); // or however you obtain it
        final boolean modern   = protocol >= 736;           // 1.16 and newer
        final boolean hasTrust = protocol <= 762;           // up to 1.19.4

        int sectionX, sectionY, sectionZ;

        if (modern) {
            long encodedPos = ByteBufHelper.readLong(byteBuf);

            sectionX = (int) (encodedPos >>> 42);
            sectionY = (int) (encodedPos << 44 >>> 44);
            sectionZ = (int) (encodedPos << 22 >>> 42);

            if (hasTrust) {
                ByteBufHelper.skipBytes(byteBuf, 1);          // trustEdges  -- ignore
            }
        } else {
            sectionX = ByteBufHelper.readInt(byteBuf);
            sectionZ = ByteBufHelper.readInt(byteBuf);
            sectionY = 0;
        }

        /* ------------------------------------------------------------------
         * 2. ---  How many block records follow? ----------------------------
         * ------------------------------------------------------------------ */
        final int recordCount = ByteBufHelper.readVarInt(byteBuf);

        /* ------------------------------------------------------------------
         * 3. ---  Allocate ONE flat int[]    --------------------------------
         *         layout per record:  x | y | z | id
         *         so index = i << 2, index+1, index+2, index+3
         * ------------------------------------------------------------------ */
        final int[] records = new int[recordCount * 4];

        /* ------------------------------------------------------------------
         * 4. ---  Decode the block list, do the distance check on the fly ---
         * ------------------------------------------------------------------ */
        boolean shouldSendTransaction = false;

        for (int i = 0; i < recordCount; i++) {

            int x, y, z, id;

            if (modern) {                                   // 1.16+
                // TODO add to ByteBufHelper in PE
                long data = readVarLong(((ByteBuf) byteBuf));

                int localX = (int) ((data >>>  8) & 0xF);
                int localZ = (int) ((data >>>  4) & 0xF);
                int localY = (int)  (data         & 0xF);

                id = (int) (data >>> 12);

                x = (sectionX << 4) | localX;
                y = (sectionY << 4) | localY;
                z = (sectionZ << 4) | localZ;

            } else {                                        // 1.15-
                short pos = ByteBufHelper.readShort(byteBuf);

                int localX =  (pos >>> 12) & 0xF;
                int localZ =  (pos >>>  8) & 0xF;
                int localY =   pos         & 0xFF;

                id = ByteBufHelper.readVarInt(byteBuf);

                x = (sectionX << 4) | localX;
                y =  localY;
                z = (sectionZ << 4) | localZ;
            }

            int base = i << 2;          // i * 4
            records[base] = x;
            records[base + 1] = y;
            records[base + 2] = z;
            records[base + 3] = id;

            // Near-player test (only until we find one match)
            if (!shouldSendTransaction &&
                    Math.abs(x - player.x) < range &&
                    Math.abs(y - player.y) < range &&
                    Math.abs(z - player.z) < range &&
                    player.lastTransSent + 2 < nowMillis) {

                shouldSendTransaction = true;
            }
        }

        /* ------------------------------------------------------------------
         * 5. ---  Fire the transaction if needed ----------------------------
         * ------------------------------------------------------------------ */
        if (shouldSendTransaction) {
            player.sendTransaction();
        }

        /* ------------------------------------------------------------------
         * 6. ---  Queue the task  -------------------------------------------
         *         The lambda captures:  (records array, recordCount)
         *         = **one reference + one int**
         * ------------------------------------------------------------------ */

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {

            int[] rec = records;    // local alias for speed

            for (int i = 0; i < rec.length; i++) {
                int base = i << 2;
                player.compensatedWorld.updateBlock(
                        rec[base],  // x
                        rec[base + 1],  // y
                        rec[base + 2],  // z
                        rec[base + 3]   // id
                );
            }
        });
    }

    public long readVarLong(ByteBuf buf) {
        long value = 0;
        int size = 0;
        int b;
        while (((b = buf.readByte()) & 0x80) == 0x80) {
            value |= (long) (b & 0x7F) << (size++ * 7);
        }
        return value | ((long) (b & 0x7F) << (size * 7));
    }
}
