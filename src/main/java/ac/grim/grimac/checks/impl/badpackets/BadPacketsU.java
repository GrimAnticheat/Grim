package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsU", experimental = true)
public class BadPacketsU extends Check implements PacketCheck {

    public BadPacketsU(final GrimPlayer player) {
        super(player);
    }
}
