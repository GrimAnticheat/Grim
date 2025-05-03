package me.grim.bench.blockchange.no_waste_bit_packing;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;
import me.grim.bench.blockchange.VersionedMultiBlockChangeHandler;
import me.grim.bench.blockchange.multi_block_bit_repack.V1200MultiBlockChangeBitRepackHandler;

import static me.grim.bench.blockchange.VersionedMultiBlockChangeHandler.RANGE;
import static me.grim.bench.blockchange.VersionedMultiBlockChangeHandler.TRANSACTION_COOLDOWN_MS;

public class MultiBlockChangeNoWasteBitPack extends AbstractBenchmarkBlockChangeHandler {

    private final static VersionedMultiBlockChangeHandler versionedMultiBlockChangeHandler = new V1200MultiBlockChangeNoWasteBitPackHandler();

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
