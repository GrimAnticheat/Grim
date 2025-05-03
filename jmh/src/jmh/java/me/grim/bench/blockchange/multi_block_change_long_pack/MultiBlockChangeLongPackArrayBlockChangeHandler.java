package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;

public class MultiBlockChangeLongPackArrayBlockChangeHandler extends AbstractBenchmarkBlockChangeHandler {

    private static final int RANGE = 16;
    private static final long TRANSACTION_COOLDOWN_MS = 2; // In milliseconds
    private final static VersionedMultiBlockChangeHandler = new Pre

    public void handleBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange(event);

        Vector3i blockPosition = blockChange.getBlockPosition();
        // Don't spam transactions (block changes are sent in batches)
        if (Math.abs(blockPosition.getX() - player.x) < RANGE && Math.abs(blockPosition.getY() - player.y) < RANGE && Math.abs(blockPosition.getZ() - player.z) < RANGE &&
                player.lastTransSent + TRANSACTION_COOLDOWN_MS < System.currentTimeMillis())
            player.sendTransaction();

        int x = blockPosition.getX();
        int y = blockPosition.getY();
        int z = blockPosition.getZ();
        int blockId = blockChange.getBlockId();

        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedWorld.updateBlock(x, y, z, blockId));
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);

        final WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = multiBlockChange.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : blocks) {
            // Don't send a transaction unless it's within 16 blocks of the player
            if (Math.abs(blockChange.getX() - player.x) < RANGE && Math.abs(blockChange.getY() - player.y) < RANGE && Math.abs(blockChange.getZ() - player.z) < RANGE && player.lastTransSent + 2 < System.currentTimeMillis()) {
                player.sendTransaction();
                break;
            }
        }

        // Create a single int array to store all block changes
        int[] blockData = new int[blocks.length * 4];
        for (int i = 0; i < blocks.length; i++) {
            WrapperPlayServerMultiBlockChange.EncodedBlock blockChange = blocks[i];
            int base = i * 4;
            blockData[base] = blockChange.getX();
            blockData[base + 1] = blockChange.getY();
            blockData[base + 2] = blockChange.getZ();
            blockData[base + 3] = blockChange.getBlockId();
        }


        // Add a single runnable to prevent excessive memory use when there are lots of block changes
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            for (int i = 0; i < blockData.length; i += 4) {
                player.compensatedWorld.updateBlock(
                        blockData[i],      // x
                        blockData[i + 1],  // y
                        blockData[i + 2],  // z
                        blockData[i + 3]   // id
                );
            }
        });
    }
}
