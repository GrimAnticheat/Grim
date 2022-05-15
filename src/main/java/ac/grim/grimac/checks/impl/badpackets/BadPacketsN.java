package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsN")
public class BadPacketsN extends PacketCheck {
    public BadPacketsN(GrimPlayer playerData) {
        super(playerData);
    }
}
