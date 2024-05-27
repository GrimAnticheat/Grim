package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "FabricatedPlace")
public class FabricatedPlace extends BlockPlaceCheck {
    public FabricatedPlace(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final Vector3f cursor = place.getCursor();
        if (cursor == null) return;

        final double allowed = Materials.isShapeExceedsCube(place.getPlacedAgainstMaterial()) || place.getPlacedAgainstMaterial() == StateTypes.LECTERN ? 1.5 : 1;
        final double minAllowed = 1 - allowed;

        final boolean lessThanMin = cursor.getX() < minAllowed || cursor.getY() < minAllowed || cursor.getZ() < minAllowed;
        final boolean greaterThanMax = cursor.getX() > allowed || cursor.getY() > allowed || cursor.getZ() > allowed;
        if (!lessThanMin && !greaterThanMax) return;

        if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }
}
