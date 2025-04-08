package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "MultiActionsC", description = "Clicked in inventory while sprinting", experimental = true)
public class MultiActionsC extends AbstractPacketCheck {
    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    private boolean serverOpenedInventoryThisTick;

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (player.isSprinting && !player.isSwimming && !serverOpenedInventoryThisTick && flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.CLICK_WINDOW);
        registry.registerHandler(event -> {
            if (!isTickPacket(event.getPacketType())) return;
            serverOpenedInventoryThisTick = false;
        });

    }

    @Override
    protected void registerSendHandlers(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(event -> player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> serverOpenedInventoryThisTick = true), PacketType.Play.Server.OPEN_WINDOW);
    }
}
