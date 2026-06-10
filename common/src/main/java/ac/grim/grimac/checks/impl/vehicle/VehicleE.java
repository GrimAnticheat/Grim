package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "VehicleE", stableKey = "grim.vehicle.spoofed_boat", experimental = true, description = "Sent boat paddle states while not in a boat", verboseVersion = 1)
public class VehicleE extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of("present:bool", "entity:vi");

    public VehicleE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_BOAT) {
            final EntityType vehicle = player.getVehicleType();

            if (!EntityTypes.isTypeInstanceOf(vehicle, EntityTypes.BOAT)) {
                if (flagAndAlert(V.write(verbose()).bool(vehicle != null).vi(vehicle == null ? 0 : vehicle.getId(player.getClientVersion()))) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
