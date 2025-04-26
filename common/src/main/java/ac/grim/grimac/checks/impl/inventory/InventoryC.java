package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.InventoryCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;

@CheckData(name = "InventoryC", setback = 3, description = "Placed a block while inventory is open")
public class InventoryC extends InventoryCheck {

    public InventoryC(GrimPlayer player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
        // It is not possible to place a block while the inventory is open
        if (player.hasInventoryOpen) {
            if (flagAndAlert()) {
                if (shouldModifyPackets()) {
                    place.resync();
                }
                if (!isNoSetbackPermission()) {
                    closeInventory();
                }
            }
        } else {
            reward();
        }
    }
}
