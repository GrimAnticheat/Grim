package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "VehicleB", stableKey = "grim.vehicle.spoofed_vehicle", description = "Claimed to be in a vehicle while not in a vehicle")
public class VehicleB extends Check implements PacketListener {
    public VehicleB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            if (!player.inVehicle() && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.STEER_VEHICLE);
    }
}
