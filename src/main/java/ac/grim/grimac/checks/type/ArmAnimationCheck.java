package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.ArmAnimationUpdate;

public interface ArmAnimationCheck extends AbstractCheck {

    default void process(final ArmAnimationUpdate armAnimationUpdate) {
    }

}
