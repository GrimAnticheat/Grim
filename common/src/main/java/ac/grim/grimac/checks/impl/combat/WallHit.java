package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "WallHit", configName = "WallHit", setback = 20)
public class WallHit extends Check implements PacketCheck {
    public WallHit(GrimPlayer player) {
        super(player);
    }
}
