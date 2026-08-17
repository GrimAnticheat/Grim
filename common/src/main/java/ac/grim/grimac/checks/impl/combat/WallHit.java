package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "WallHit", stableKey = "grim.combat.wall_hit", description = "Attacked an entity through a wall", configName = "WallHit", setback = 20)
public class WallHit extends Check implements PacketCheck {
    public WallHit(GrimPlayer player) {
        super(player);
    }
}
