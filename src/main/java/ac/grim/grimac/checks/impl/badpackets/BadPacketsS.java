package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsS", checkType = CheckType.PACKETS)
public class BadPacketsS extends Check implements PacketCheck {
    public BadPacketsS(GrimPlayer player) {
        super(player);
    }

}
