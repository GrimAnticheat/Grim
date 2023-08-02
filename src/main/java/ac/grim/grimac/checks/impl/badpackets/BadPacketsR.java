package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;

@CheckData(name = "BadPacketsR")
public class BadPacketsR extends Check implements PacketCheck {

    private short lastConfirm = Short.MAX_VALUE;

    public BadPacketsR(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            final WrapperPlayClientWindowConfirmation packet = new WrapperPlayClientWindowConfirmation(event);

            final short actionId = packet.getActionId();
            if (packet.getWindowId() == 0 && player.didWeSendThatTrans.contains(actionId) && actionId < 0) {
                if (lastConfirm != Short.MAX_VALUE && actionId != lastConfirm - 1) {
                    flagAndAlert("action=" + actionId);
                }
                lastConfirm = actionId;
            }
        }
    }
}
