package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public class BlockBreakCheck extends Check {
    public BlockBreakCheck(GrimPlayer player) {
        super(player);
    }

    public void onBlockBreak(final BlockBreak blockBreak) {}
}
