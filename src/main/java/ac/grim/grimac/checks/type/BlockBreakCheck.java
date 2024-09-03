package ac.grim.grimac.checks.type;

import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public interface BlockBreakCheck extends PacketCheck {
    void onBlockBreak(final BlockBreak blockBreak);
}
