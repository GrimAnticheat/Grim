package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimL", description = "Repeated exact one-step yaw increments", decay = 0.02, experimental = true)
public class AimL extends Check implements RotationCheck {
    private int repeated;
    private float lastYaw;

    public AimL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(350) || player.packetStateData.lastPacketWasTeleport) {
            repeated = 0;
            lastYaw = rotationUpdate.getDeltaXRotABS();
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        if (yaw > 0.35F && Math.abs(yaw - lastYaw) < 1.0E-5F) {
            if (++repeated > 12) {
                flagAndAlert("yaw=" + yaw + ", repeated=" + repeated);
            }
        } else {
            repeated = Math.max(0, repeated - 1);
            reward();
        }

        lastYaw = yaw;
    }
}
