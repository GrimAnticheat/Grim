package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "CrashF")
public class CrashF extends AbstractPacketCheck {

    public CrashF(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            int clickType = click.getWindowClickType().ordinal();
            int button = click.getButton();
            int windowId = click.getWindowId();
            int slot = click.getSlot();

            if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
                if (flagAndAlert("clickType=" + clickType + " button=" + button)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }

            else if (windowId >= 0 && clickType == 2 && slot < 0) {
                if (flagAndAlert("clickType=" + clickType + " button=" + button + " slot=" + slot)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.CLICK_WINDOW);
    }
}
