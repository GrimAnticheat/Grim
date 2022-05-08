package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "CrashD")
public class CrashD extends PacketCheck {
    public CrashD(GrimPlayer playerData) {
        super(playerData);
    }
}
