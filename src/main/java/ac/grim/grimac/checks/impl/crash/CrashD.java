package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

@CheckData(name = "CrashD", experimental = true)
public class CrashD extends PacketCheck {

    public CrashD(GrimPlayer playerData) {
        super(playerData);
    }

    private int type = -1;
    private int lecternId = -1;

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);
            this.type = window.getType();
            if (type == 16) lecternId = window.getContainerId();
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            int windowId = click.getWindowId();
            if (type == 16 && windowId > 0 && windowId == lecternId) {
                if (flagAndAlert()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
