package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsW", stableKey = "grim.badpackets.invalid_entity_target", description = "Interacted with non-existent entity", experimental = true)
public class BadPacketsW extends Check {
    public BadPacketsW(GrimPlayer player) {
        super(player);
    }
}
