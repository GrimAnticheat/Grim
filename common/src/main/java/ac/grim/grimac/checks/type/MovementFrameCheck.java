package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.MovementFrame;

public interface MovementFrameCheck extends AbstractCheck {
    default void onMovementFrame(final MovementFrame frame) {
    }
}
