package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.type.VehicleCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.data.PredictionData;

public class VehiclePredictionRunner extends VehicleCheck {
    public VehiclePredictionRunner(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final VehiclePositionUpdate vehicleUpdate) {
        PredictionData data = new PredictionData(player, vehicleUpdate.getTo().getX(), vehicleUpdate.getTo().getY(), vehicleUpdate.getTo().getZ(), vehicleUpdate.getXRot(), vehicleUpdate.getYRot(), vehicleUpdate.isTeleport());
        player.movementCheckRunner.processAndCheckMovementPacket(data);
    }
}
