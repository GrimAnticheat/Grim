package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;

public class PositionCheck extends Check {

    public PositionCheck(final GrimPlayer playerData) {
        super(playerData);
    }

    public void onPositionUpdate(final PositionUpdate positionUpdate) {

    }
}
