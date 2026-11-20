package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "VehicleA", stableKey = "grim.vehicle.impossible_input", description = "Impossible input values")
public class VehicleA extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("forwards={f32}, sideways={f32}");

    public VehicleA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            final WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

            if (!Float.isFinite(packet.getForward()) || !Float.isFinite(packet.getSideways())
                    || Math.abs(packet.getForward()) > 0.98f || Math.abs(packet.getSideways()) > 0.98f) {
                if (flag(V.write(verbose()).f32(packet.getForward()).f32(packet.getSideways())) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.STEER_VEHICLE);
    }
}
