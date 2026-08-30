package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "InvalidInteractTarget", stableKey = "grim.badpackets.invalid_entity_target", description = "Interacted with non-existent entity", experimental = true)
public class InvalidInteractTarget extends Check {
    public InvalidInteractTarget(GrimPlayer player) {
        super(player);
    }
}
