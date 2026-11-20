package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "VehicleE", stableKey = "grim.vehicle.spoofed_boat", experimental = true, description = "Sent boat paddle states while not in a boat")
public class VehicleE extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("vehicle=[{entity}|null]");

    public VehicleE(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            final EntityType vehicle = player.getVehicleType();

            if (EntityTypes.isTypeInstanceOf(vehicle, EntityTypes.BOAT)) {
                return;
            }

            int entityTypeId = vehicle == null ? 0 : VerboseCodecs.entity(vehicle, player.getClientVersion());
            if (flag(V.write(verbose()).bool(vehicle != null).uint(entityTypeId)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.STEER_BOAT);
    }
}
