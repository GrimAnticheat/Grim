package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsN", setback = 0)
public class BadPacketsN extends Check {
    public BadPacketsN(final GrimPlayer player) {
        super(player);
    }
}
