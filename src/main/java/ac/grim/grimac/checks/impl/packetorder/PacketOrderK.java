package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderK", experimental = true)
public class PacketOrderK extends Check implements PacketCheck {
    public PacketOrderK(final GrimPlayer player) {
        super(player);
    }

    private boolean opened, clickedOrClosed;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                opened = true;
                if (clickedOrClosed) {
                    flagAndAlert();
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW || event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            clickedOrClosed = true;
            if (opened && flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            opened = clickedOrClosed = false;
        }
    }
}
