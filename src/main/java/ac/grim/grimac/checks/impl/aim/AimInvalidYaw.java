package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimInvalidYaw")
public class AimInvalidYaw extends RotationCheck {

    public AimInvalidYaw(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport) return;

        boolean invalid = Math.abs(rotationUpdate.getDeltaPitch()) > 0.5 && Math.abs(rotationUpdate.getDeltaYaw()) < 0.001 && rotationUpdate.getDeltaYaw() != 0;

        if (invalid) {
            flagAndAlert("x: " + Math.abs(rotationUpdate.getDeltaYaw()) + " y: " + Math.abs(rotationUpdate.getDeltaPitch()));
        }
    }
}
