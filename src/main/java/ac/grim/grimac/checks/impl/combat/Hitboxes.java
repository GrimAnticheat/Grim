package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Hitboxes", setback = 10)
public class Hitboxes extends AbstractPacketCheck {
    public Hitboxes(GrimPlayer player) {
        super(player);
    }
}
