package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Hitboxes", stableKey = "grim.combat.hitboxes", description = "Tried to hit an entity outside its valid hitbox", setback = 10)
public class Hitboxes extends Check {
    public Hitboxes(GrimPlayer player) {
        super(player);
    }
}
