package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "InvalidPlace")
public class InvalidPlace extends BlockPlaceCheck {
    public InvalidPlace(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final Vector3f cursor = place.getCursor();
        if (cursor == null) return;
        if (Float.isFinite(cursor.getX()) && Float.isFinite(cursor.getY()) && Float.isFinite(cursor.getZ())) return;

        if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }
}
