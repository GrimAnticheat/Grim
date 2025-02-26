package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

@CheckData(name = "BadPacketsP", experimental = true)
public class BadPacketsP extends AbstractPacketCheck {

    public BadPacketsP(GrimPlayer playerData) {
        super(playerData);
    }

    private int containerType = -1;
    private int containerId = -1;

    @Override
    protected void registerSendHandlers(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);
            this.containerType = window.getType();
            this.containerId = window.getContainerId();
        }, PacketType.Play.Server.OPEN_WINDOW);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            WindowClickType clickType = wrapper.getWindowClickType();
            int button = wrapper.getButton();

            // TODO: Adjust for containers
            boolean flag = switch (clickType) {
                case PICKUP, QUICK_MOVE, CLONE -> button > 2 || button < 0;
                case SWAP -> (button > 8 || button < 0) && button != 40;
                case THROW -> button != 0 && button != 1;
                case QUICK_CRAFT -> button == 3 || button == 7 || button > 10 || button < 0;
                case PICKUP_ALL -> button != 0;
                case UNKNOWN -> false;
            };

            // Allowing this to false flag to debug and find issues faster
            if (flag) {
                if (flagAndAlert("clickType=" + clickType.toString().toLowerCase() + ", button=" + button + (wrapper.getWindowId() == containerId ? ", container=" + containerType : "")) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.CLICK_WINDOW);
    }
}
