package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "EntityPierce", stableKey = "grim.combat.entity_pierce", description = "Attacked an entity through another entity", configName = "EntityPierce", setback = 30)
public class EntityPierce extends Check implements PacketCheck {
    public EntityPierce(GrimPlayer player) {
        super(player);
    }
}
