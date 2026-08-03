package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public interface PostFlyingBlockBreakListener extends AbstractCheck {
    void onPostFlyingBlockBreak(BlockBreak blockBreak);
}
