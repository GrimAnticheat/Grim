package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.checks.type.BlockPlaceListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;

@CheckData(name = "MultiActionsK", stableKey = "grim.multiactions.inventory_place", description = "Placed a block while in an inventory", experimental = true)
public class MultiActionsK extends BlockPlaceCheck implements BlockPlaceListener {

    public MultiActionsK(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.openWindow.mustBeOpen() && flag() && shouldModifyPackets() && shouldCancel()) {
            place.resync();
            player.closeInventory();
        }
    }
}
