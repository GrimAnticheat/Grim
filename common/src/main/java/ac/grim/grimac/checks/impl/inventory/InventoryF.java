package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.InventoryCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryDesyncStatus;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "InventoryF", setback = 3, description = "Sent a click window packet without a open inventory", experimental = true)
public class InventoryF extends InventoryCheck {

    public InventoryF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Exempt on 1.9+ server version due to the Via hack done in PacketPlayerWindow, the exemption can be deleted
        // once we are ahead of ViaVersion
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) return;

        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (!player.hasInventoryOpen && player.inventoryDesyncStatus == InventoryDesyncStatus.NOT_DESYNCED) {
                if (flagAndAlert()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    if (!isNoSetbackPermission()) {
                        closeInventory();
                    }
                }
            } else {
                reward();
            }
        }
    }
}
