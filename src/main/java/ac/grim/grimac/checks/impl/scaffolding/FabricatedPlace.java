package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "Fabricated Place")
public class FabricatedPlace extends BlockPlaceCheck {
    public FabricatedPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.getCursor();
        if (cursor == null) return;

        if (cursor.getX() < 0 || cursor.getY() < 0 || cursor.getZ() < 0 || cursor.getX() > 1 || cursor.getY() > 1 || cursor.getZ() > 1) {
            flagAndAlert();
        }
    }
}
