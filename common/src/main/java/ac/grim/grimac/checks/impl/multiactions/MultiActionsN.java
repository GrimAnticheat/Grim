package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsN", stableKey = "grim.multiactions.inventory_inventory", description = "Opened an inventory while in an inventory", experimental = true)
public class MultiActionsN extends Check implements PreViaPacketReceiveListener {

    public MultiActionsN(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getAction() == WrapperPlayClientEntityAction.Action.OPEN_HORSE_INVENTORY) {
                onOpen(event);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus packet = new WrapperPlayClientClientStatus(event);
            if (packet.getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT && player.getClientVersion().isOlderThan(ClientVersion.V_1_12)) {
                onOpen(event);
            }
        }
    }

    private void onOpen(@NotNull PacketReceiveEvent event) {
        if (player.openWindow.mustBeOpen()
                && player.openWindow.getTicksOpen() != 0 // very hard, but technically possible
                && flag()
                && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
