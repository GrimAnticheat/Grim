package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "MultiActionsG", stableKey = "grim.multiactions.action_while_rowing", description = "Attacking or using items while rowing a boat", experimental = true)
public class MultiActionsG extends BlockPlaceCheck {
    private static final Verbose V =
            Verbose.of("action=interact").or("action=attack").or("action=spectateEntity")
                    .or("action=use").or("action=place"); // shape index == ACTION_* value

    private static final int ACTION_INTERACT = 0;
    private static final int ACTION_ATTACK = 1;
    private static final int ACTION_SPECTATE_ENTITY = 2;
    private static final int ACTION_USE = 3;
    private static final int ACTION_PLACE = 4;

    public MultiActionsG(GrimPlayer player) {
        super(player);
    }

    private Verbose.Writer writeAction(int action) {
        return V.write(verbose(), action);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && isCheckActive()
                && flag(writeAction(ACTION_INTERACT)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (event.getPacketType() == PacketType.Play.Client.ATTACK && isCheckActive()
                && flag(writeAction(ACTION_ATTACK)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY && isCheckActive()
                && flag(writeAction(ACTION_SPECTATE_ENTITY)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isCheckActive()
                && flag(writeAction(ACTION_USE)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        int action = place.getFace() == BlockFace.OTHER ? ACTION_USE : ACTION_PLACE;
        if (isCheckActive() && flag(writeAction(action)) && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }

    public boolean isCheckActive() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !player.vehicleData.wasVehicleSwitch // one tick off?
                && player.inVehicle() && player.compensatedEntities.self.getRiding().getType().isInstanceOf(EntityTypes.BOAT)
                && (player.vehicleData.nextVehicleForward != 0 || player.vehicleData.nextVehicleHorizontal != 0);
    }
}
