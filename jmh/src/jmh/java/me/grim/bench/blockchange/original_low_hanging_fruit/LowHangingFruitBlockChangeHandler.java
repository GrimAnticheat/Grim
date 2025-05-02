package me.grim.bench.blockchange.original_low_hanging_fruit;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;

public class LowHangingFruitBlockChangeHandler extends AbstractBenchmarkBlockChangeHandler {

    private static final int RANGE = 16;
    private static final long TRANSACTION_COOLDOWN_MS = 2; // In milliseconds

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

        // Add a single runnable to prevent excessive memory use when there are lots of block changes
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : blocks) {
                player.compensatedWorld.updateBlock(blockChange.getX(), blockChange.getY(), blockChange.getZ(), blockChange.getBlockId());
            }
        });
    }
}
