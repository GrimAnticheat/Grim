package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NoSlowB", setback = 5)
public class NoSlowB extends Check implements PacketCheck {

    public NoSlowB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        // Players can sprint if they're able to fly (MCP)
        if (player.canFly) return;

        if (player.food >= 6.0F || !player.isSprinting) {
            reward();
            return;
        }

        if (!flag()) return;

        // Cancel the packet
        if (shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
        alert("");
        player.getSetbackTeleportUtil().executeNonSimulatingSetback();
    }
}