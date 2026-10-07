package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

// TODO: 0.03?
@CheckData(name = "MultiActionsR", stableKey = "grim.multiactions.inventory_portal", description = "Clicking in inventory while in a nether portal", experimental = true)
public class MultiActionsR extends Check implements PreViaPacketReceiveListener {

    public MultiActionsR(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
                && player.intersectedWithNetherPortal
                && flag() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
            player.closeInventory();
        }
    }
}
