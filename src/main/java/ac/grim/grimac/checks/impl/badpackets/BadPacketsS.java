package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsS")
public class BadPacketsS extends AbstractPacketCheck {
    public BadPacketsS(GrimPlayer player) {
        super(player);
    }

}
