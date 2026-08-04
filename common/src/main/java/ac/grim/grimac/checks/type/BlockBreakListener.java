package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public interface BlockBreakListener extends AbstractCheck {
    void onBlockBreak(BlockBreak blockBreak);
}
