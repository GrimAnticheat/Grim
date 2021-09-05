package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.packettype.PacketType;

@CheckData(name = "Timer - Vehicle", configName = "TimerVehicle", flagCooldown = 1000, maxBuffer = 5)
public class VehicleTimer extends TimerCheck {
    public VehicleTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean checkReturnPacketType(byte packetType) {
        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        return packetType != PacketType.Play.Client.VEHICLE_MOVE || player.packetStateData.lastPacketWasTeleport;
    }
}
