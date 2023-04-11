package ac.grim.grimac.checks.type;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
