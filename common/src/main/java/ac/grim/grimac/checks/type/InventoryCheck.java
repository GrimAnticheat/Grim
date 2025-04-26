package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public class InventoryCheck extends BlockPlaceCheck implements PacketCheck {
    // Impossible transaction ID
    protected static final long NONE = Long.MAX_VALUE;

    protected long closeTransaction = NONE;
    protected int closePacketsToSkip;

    public InventoryCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    @MustBeInvokedByOverriders
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            // Disallow any clicks if inventory is closing
            if (closeTransaction != NONE && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.getInventory().needResend = true;
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            // Players with high ping can close inventory faster than send transaction back
            if (closeTransaction != NONE && closePacketsToSkip-- <= 0) {
                closeTransaction = NONE;
            }
        }
    }

    public void closeInventory() {
        if (closeTransaction != NONE) {
            return;
        }

        int windowId = player.getInventory().openWindowID;

        player.user.writePacket(new WrapperPlayServerCloseWindow(windowId));

        // Force close inventory on server side
        closePacketsToSkip = 1; // Sending close packet to itself, so skip it
        PacketEvents.getAPI().getProtocolManager().receivePacket(
                player.user.getChannel(), new WrapperPlayClientCloseWindow(windowId)
        );

        player.sendTransaction();

        int transaction = player.lastTransactionSent.get();
        closeTransaction = transaction;
        player.latencyUtils.addRealTimeTask(transaction, () -> {
            if (closeTransaction == transaction) {
                closeTransaction = NONE;
            }
        });

        player.user.flushPackets();
    }
}
