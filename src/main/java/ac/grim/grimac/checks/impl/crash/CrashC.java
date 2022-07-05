package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "CrashC")
public class CrashC extends PacketCheck {
    public CrashC(GrimPlayer playerData) {
        super(playerData);
    }
}
