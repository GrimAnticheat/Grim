package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "CrashC")
public class CrashC extends Check implements PacketCheck {
    public CrashC(final GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        final WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;

        final Location pos = flying.getLocation();
        if (Double.isFinite(pos.getX()) && Double.isFinite(pos.getY()) && Double.isFinite(pos.getZ()) &&
                Float.isFinite(pos.getYaw()) && Float.isFinite(pos.getPitch())) return;

        flagAndAlert("xyzYP: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + pos.getYaw() + ", " + pos.getPitch());
        player.getSetbackTeleportUtil().executeViolationSetback();
        event.setCancelled(true);
        player.onPacketCancel();
    }
}
