package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "CrashC", stableKey = "grim.crash.nan_position", verboseVersion = 1, description = "Sent non-finite position or rotation")
public class CrashC extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "x:f64", "y:f64", "z:f64", "yaw:f32", "pitch:f32");

    public CrashC(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (flying.hasPositionChanged()) {
                Location pos = flying.getLocation();
                if (!Double.isFinite(pos.getX()) || !Double.isFinite(pos.getY()) || !Double.isFinite(pos.getZ())
                    || !Float.isFinite(pos.getYaw()) || !Float.isFinite(pos.getPitch())
                   ) {
                    flagAndAlert(V.write(verbose()).f64(pos.getX()).f64(pos.getY()).f64(pos.getZ()).f32(pos.getYaw()).f32(pos.getPitch()));
                    executeViolationSetback();
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
