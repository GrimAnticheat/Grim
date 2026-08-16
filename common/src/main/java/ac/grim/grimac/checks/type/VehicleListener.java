package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleListener extends AbstractCheck {
    void process(final VehiclePositionUpdate vehicleUpdate);
}
