package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(name = "Timer - Vehicle", configName = "TimerVehicle", setback = 10)
public class VehicleTimer extends TimerCheck {
    public VehicleTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean checkReturnPacketType(PacketTypeCommon packetType) {
        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        return packetType != PacketType.Play.Client.VEHICLE_MOVE || player.packetStateData.lastPacketWasTeleport;
    }
}
