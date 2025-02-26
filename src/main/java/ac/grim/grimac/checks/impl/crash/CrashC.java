package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "CrashC", description = "Sent non-finite position or rotation")
public class CrashC extends AbstractPacketCheck {
    public CrashC(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (flying.hasPositionChanged()) {
                Location pos = flying.getLocation();
                if (Double.isNaN(pos.getX()) || Double.isNaN(pos.getY()) || Double.isNaN(pos.getZ())
                        || Double.isInfinite(pos.getX()) || Double.isInfinite(pos.getY()) || Double.isInfinite(pos.getZ()) ||
                        Float.isNaN(pos.getYaw()) || Float.isNaN(pos.getPitch()) ||
                        Float.isInfinite(pos.getYaw()) || Float.isInfinite(pos.getPitch())) {
                    flagAndAlert("xyzYP: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + pos.getYaw() + ", " + pos.getPitch());
                    player.getSetbackTeleportUtil().executeViolationSetback();
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, this::isFlying);
    }
}
