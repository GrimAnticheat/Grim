package ac.grim.grimac.events.packets.worldreader.multiblockchange;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;

public class LegacyMultiBlockChangeHandler implements VersionedMultiBlockChangeHandler {

    // TODO hande optimize Pre 1.16 code to also reduce memory usage and not use wrapper
    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange multiBlockChange = new WrapperPlayServerMultiBlockChange(event);

        final var blocks = multiBlockChange.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock blockChange : blocks) {
            // Don't send a transaction unless it's within 16 blocks of the player
            if (Math.abs(blockChange.getX() - player.x) < RANGE && Math.abs(blockChange.getY() - player.y) < RANGE && Math.abs(blockChange.getZ() - player.z) < RANGE && player.lastTransSent + TRANSACTION_COOLDOWN_MS < System.currentTimeMillis()) {
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
