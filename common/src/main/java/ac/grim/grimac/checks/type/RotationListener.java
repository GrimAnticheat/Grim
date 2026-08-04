package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

public interface RotationListener extends AbstractCheck {
    void process(RotationUpdate rotationUpdate);
}
