package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "MultiActionsL", stableKey = "grim.multiactions.inventory_break", description = "Breaking a block while in an inventory", experimental = true)
public class MultiActionsL extends Check implements BlockBreakListener {

    public MultiActionsL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (!player.openWindow.mustBeOpen()) return;

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING && player.openWindow.getTicksOpen() == 0) {
            return;
        }

        if (flag() && shouldModifyPackets()) {
            blockBreak.cancel();
            player.closeInventory();
        }
    }
}
