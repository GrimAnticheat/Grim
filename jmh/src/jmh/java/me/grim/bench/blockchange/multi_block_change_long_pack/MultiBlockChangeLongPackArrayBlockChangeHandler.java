package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;

public class MultiBlockChangeLongPackArrayBlockChangeHandler extends AbstractBenchmarkBlockChangeHandler {

    private static final int RANGE = 16;
    private static final long TRANSACTION_COOLDOWN_MS = 2; // In milliseconds
    private final static VersionedMultiBlockChangeHandler  versionedMultiBlockChangeHandler = new V1200MultiBlockChangeHandler();

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
        versionedMultiBlockChangeHandler.handleMultiBlockChange(player, event);
    }
}
