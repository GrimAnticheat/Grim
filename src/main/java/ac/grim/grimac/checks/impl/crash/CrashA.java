package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "CrashA")
public class CrashA extends Check implements PacketCheck {
    private static final double HARD_CODED_BORDER = 2.9999999E7D;

    public CrashA(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport || !WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        final WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);

        if (!packet.hasPositionChanged()) return;
        // Y technically is uncapped, but no player will reach these values legit
        if (Math.abs(packet.getLocation().getX()) <= HARD_CODED_BORDER &&
                Math.abs(packet.getLocation().getZ()) <= HARD_CODED_BORDER &&
                Math.abs(packet.getLocation().getY()) <= Integer.MAX_VALUE) return;

        flagAndAlert(); // Ban
        player.getSetbackTeleportUtil().executeViolationSetback();
        event.setCancelled(true);
        player.onPacketCancel();
    }
}
