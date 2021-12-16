package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

public class RotationCheck extends Check<RotationUpdate> {

    public RotationCheck(final GrimPlayer playerData) {
        super(playerData);
    }

    public void process(final RotationUpdate rotationUpdate) {
    }
}
