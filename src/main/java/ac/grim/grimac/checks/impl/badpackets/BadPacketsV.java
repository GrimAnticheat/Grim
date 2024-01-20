package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "BadPacketsV")
public class BadPacketsV extends Check implements PacketCheck {

    public BadPacketsV(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);

            click.getActionNumber().ifPresent(actionNumber -> {
                // Action number always 0 with >=1.17 client and <1.17 server because of via
                // Maybe we should change our max transaction packet id to -1?
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_17)) {
                    if (actionNumber <= 0) {
                        flagAndAlert();
                        if (shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                }
                // Or just set action number to 1 to prevent server sends <=0 transaction packets
                else if (shouldModifyPackets()) {
                    click.setActionNumber(1);
                }
            });

        }
    }

}
