package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;

public class ResultSlot extends Slot {

    public ResultSlot(AbstractContainerMenu container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(WrappedStack p_40178_) {
        return false;
    }

    @Override
    public void onTake(GrimPlayer p_150638_, WrappedStack p_150639_) {
        // TODO: We should handle crafting recipe, but the server resync's here so we should be fine for now...
    }
}
