package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsW", description = "Interacted with non-existent entity", experimental = true, checkType = CheckType.PACKETS)
public class BadPacketsW extends Check implements PacketCheck {
    public BadPacketsW(GrimPlayer player) {
        super(player);
    }
}
