package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimN", description = "Steady combat tracking with near-zero jitter", decay = 0.02, experimental = true)
public class AimN extends Check implements RotationCheck {
    private int veryLowJitterTicks;

    public AimN(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(450) || player.packetStateData.lastPacketWasTeleport || rotationUpdate.isCinematic()) {
            veryLowJitterTicks = 0;
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        float pitch = rotationUpdate.getDeltaYRotABS();
        if (yaw > 0.4F && yaw < 1.0F && pitch > 0.03F && pitch < 0.07F) {
            if (++veryLowJitterTicks > 18) {
                flagAndAlert("ticks=" + veryLowJitterTicks + ", yaw=" + yaw + ", pitch=" + pitch);
            }
        } else {
            veryLowJitterTicks = Math.max(0, veryLowJitterTicks - 1);
            reward();
        }
    }
}
