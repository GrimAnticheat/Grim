package ac.grim.grimac.checks.impl.timer;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(name = "VehicleTimer", stableKey = "grim.timer.vehicle", description = "Sent vehicle movement packets faster than the expected client tick rate", setback = 10)
public class VehicleTimer extends Timer {
    private boolean isDummy = false;

    public VehicleTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {
        // Ignore teleports, unless the client reported one in its regular tick move
        if (player.packetStateData.lastPacketWasTeleport
                && !(packetType == PacketType.Play.Client.VEHICLE_MOVE && player.vehicleData.teleportInTickMove)) return false;

        if (packetType == PacketType.Play.Client.VEHICLE_MOVE) {
            isDummy = false;
            return true; // Client controlling vehicle
        }

        if (packetType == PacketType.Play.Client.STEER_VEHICLE) {
            if (isDummy) { // Server is controlling vehicle
                return true;
            }
            isDummy = true; // Client is controlling vehicle
        }

        return false;
    }
}
