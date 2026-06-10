package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceB", stableKey = "grim.scaffolding.invalid_place_b", verboseVersion = 1, description = "Sent impossible block face id")
public class InvalidPlaceB extends BlockPlaceCheck {
    public static final VerboseSchema V = VerboseSchema.of("direction:zz");

    public InvalidPlaceB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getFaceId() == 255 && PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        if (place.getFaceId() < 0 || place.getFaceId() > 5) {
            // ban
            int direction = place.getFaceId();
            if (flagAndAlert(V.write(verbose()).zz(direction)) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
