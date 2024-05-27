package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "CrashF")
public class CrashF extends Check implements PacketCheck {

    public CrashF(final GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        final WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        final int clickType = click.getWindowClickType().ordinal(),
                button = click.getButton(),
                windowId = click.getWindowId(),
                slot = click.getSlot();

        final boolean flagged;
        if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
            flagged = flagAndAlert("clickType=" + clickType + " button=" + button);
        } else if (windowId >= 0 && clickType == 2 && slot < 0) {
            flagged = flagAndAlert("clickType=" + clickType + " button=" + button + " slot=" + slot);
        } else flagged = false;
        if (flagged) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

}
