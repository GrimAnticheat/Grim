package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimInvalidPitch")
public class AimInvalidPitch extends RotationCheck {

    public AimInvalidPitch(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport) return;

        boolean invalid = Math.abs(rotationUpdate.getTo().getYaw()) != 90 && Math.abs(rotationUpdate.getDeltaYaw()) > 0.5 && Math.abs(rotationUpdate.getDeltaPitch()) < 0.001 && rotationUpdate.getDeltaPitch() != 0;

        if (invalid) {
            flagAndAlert("x: " + Math.abs(rotationUpdate.getDeltaYaw()) + "y: " + Math.abs(rotationUpdate.getDeltaPitch()));
        }
    }
}
