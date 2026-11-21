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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "VehicleD", stableKey = "grim.vehicle.spoofed_jump", experimental = true, description = "Jumped in a vehicle that cannot jump")
public class VehicleD extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("vehicle=[{entity}|null]");

    public VehicleD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getAction() != WrapperPlayClientEntityAction.Action.START_JUMPING_WITH_HORSE) return;

            final EntityType vehicle = player.getVehicleType();

            if (!EntityTypes.isTypeInstanceOf(vehicle, EntityTypes.ABSTRACT_HORSE) && !EntityTypes.isTypeInstanceOf(vehicle, EntityTypes.ABSTRACT_NAUTILUS)) {
                int entityTypeId = vehicle == null ? 0 : VerboseCodecs.entity(vehicle, player.getClientVersion());
                if (flag(V.write(verbose()).bool(vehicle != null).uint(entityTypeId)) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.ENTITY_ACTION);
    }
}
