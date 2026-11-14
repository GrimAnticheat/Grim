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

@CheckData(name = "MultiActionsP", stableKey = "grim.multiactions.inventory_drop", description = "Dropping items without using click packets while in an inventory", experimental = true)
public class MultiActionsP extends Check implements PreViaPacketReceiveListener {

    public MultiActionsP(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (isDrop(action) && player.openWindow.mustBeOpen()
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
