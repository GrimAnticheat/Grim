package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "VehicleG", stableKey = "grim.vehicle.ignore_boat_steer", experimental = true, description = "Moving in a boat without sending paddle states")
public class VehicleG extends Check implements PacketReceiveListener {
    private boolean sent;

    public VehicleG(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_BOAT) {
            sent = true;
        } else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE && !player.packetStateData.lastPacketWasTeleport) {
            if (!EntityTypes.isTypeInstanceOf(player.getVehicleType(), EntityTypes.BOAT)) return;

            if (!sent && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            sent = false;
        }
    }
}
