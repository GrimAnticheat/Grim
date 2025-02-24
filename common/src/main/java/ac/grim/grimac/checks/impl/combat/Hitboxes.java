package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Hitboxes", setback = 10)
public class Hitboxes extends Check implements PacketCheck {
    public Hitboxes(GrimPlayer player) {
        super(player);
    }
}
