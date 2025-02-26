package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "MultiActionsD", description = "Closed inventory while sprinting", experimental = true)
public class MultiActionsD extends AbstractPacketCheck {
    public MultiActionsD(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (player.isSprinting && !player.isSwimming && flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.CLOSE_WINDOW);
    }
}
