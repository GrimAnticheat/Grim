package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "MultiActionsJ", stableKey = "grim.multiactions.inventory_use", description = "Used an item while in an inventory", experimental = true)
public class MultiActionsJ extends Check implements PreViaPacketReceiveListener {

    public MultiActionsJ(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)
                && new WrapperPlayClientPlayerBlockPlacement(event).getFaceId() == 255
                ||event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (player.openWindow.mustBeOpen() && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
