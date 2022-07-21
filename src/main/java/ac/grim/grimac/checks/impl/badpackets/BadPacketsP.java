package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "BadPacketsP", experimental = true)
public class BadPacketsP extends PacketCheck {

    public BadPacketsP(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            int state = wrapper.getWindowClickType().ordinal();
            int button = wrapper.getButton();

            boolean flag = false;

            switch (state) {
                case 0:
                case 1:
                    if (button != 0 && button != 1) flag = true;
                    break;
                case 2:
                    if ((button > 8 || button < 0) && button != 40) flag = true;
                    break;
                case 3:
                    if (button != 2) flag = true;
                    break;
                case 4:
                case 5:
                    if (button == 3 || button == 7 || button > 10 || button < 0) flag = true;
                    break;
                case 6:
                    if (button != 0) flag = true;
                    break;
            }
            //TODO: Potentially cancel packet once we guarantee this doesn't false on all versions
            if (flag) {
                flagAndAlert("state=" + state + " button=" + button);
            }

        }
    }

}
