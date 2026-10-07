package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsQ", stableKey = "grim.multiactions.inventory_swap", description = "Swapping items with offhand without using click packets while in an inventory", experimental = true)
public class MultiActionsQ extends Check implements PreViaPacketReceiveListener {

    public MultiActionsQ(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (action == DiggingAction.SWAP_ITEM_WITH_OFFHAND && player.openWindow.mustBeOpen()
                    && flag() && shouldModifyPackets()) {
                if (canCancel(action)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                player.closeInventory();
            }
        }
    }
}
