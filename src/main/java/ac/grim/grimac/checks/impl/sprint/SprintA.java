package ac.grim.grimac.checks.impl.sprint;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(name = "SprintA", description = "Sprinting with too low hunger", setback = 0)
public class SprintA extends AbstractPacketCheck {

    public SprintA(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            // Players can sprint if they're able to fly (MCP)
            if (player.canFly) return;

            if (player.food < 6.0F && player.isSprinting) {
                if (flagAndAlert()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    if (shouldSetback()) {
                        player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                    }
                }
            } else {
                reward();
            }
        }, this::isFlying);
    }
}
