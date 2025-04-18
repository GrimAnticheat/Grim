package ac.grim.grimac.checks.impl.sprint;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SprintA", description = "Sprinting with too low hunger", setback = 0, checkType = CheckType.MOVEMENT)
public class SprintA extends Check implements PacketCheck {

    public SprintA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
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
        }
    }
}
