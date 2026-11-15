package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;

@CheckData(name = "CrashC", stableKey = "grim.crash.nan_position", description = "Sent non-finite position or rotation")
public class CrashC extends Check implements PreViaPacketReceiveListener {
    private static final Verbose V =
            Verbose.of("xyzYP={f64}, {f64}, {f64}, {f32}, {f32}");

    public CrashC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            Location pos = flying.getLocation();

            boolean invalidPosition = flying.hasPositionChanged() && (!Double.isFinite(pos.getX()) || !Double.isFinite(pos.getY()) || !Double.isFinite(pos.getZ()));
            boolean invalidRotation = flying.hasRotationChanged() && (!Float.isFinite(pos.getYaw()) || !Float.isFinite(pos.getPitch()));
            if (!invalidPosition && !invalidRotation) {
                return;
            }

            flagAndCancel(event, pos);
        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            WrapperPlayClientVehicleMove move = new WrapperPlayClientVehicleMove(event);
            if (Double.isFinite(move.getPosition().getX()) && Double.isFinite(move.getPosition().getY()) && Double.isFinite(move.getPosition().getZ()) && Float.isFinite(move.getYaw()) && Float.isFinite(move.getPitch())) {
                return;
            }

            Location pos = new Location(move.getPosition(), move.getYaw(), move.getPitch());
            flagAndCancel(event, pos);
        }
    }

    private void flagAndCancel(PacketReceiveEvent event, Location pos) {
        flag(V.write(verbose()).f64(pos.getX()).f64(pos.getY()).f64(pos.getZ()).f32(pos.getYaw()).f32(pos.getPitch()));
        executeViolationSetback();
        event.setCancelled(true);
        player.onPacketCancel();
    }

}
