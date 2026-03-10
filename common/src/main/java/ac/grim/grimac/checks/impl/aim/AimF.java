package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimF", description = "Repeated large yaw flicks with tiny pitch correction", decay = 0.02, experimental = true)
public class AimF extends Check implements RotationCheck {
    private int streak;

    public AimF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle() || !player.actionManager.hasAttackedSince(250)) {
            streak = 0;
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        float pitch = rotationUpdate.getDeltaYRotABS();
        if (yaw > 25.0F && pitch < 0.08F && !rotationUpdate.isCinematic()) {
            if (++streak > 5) {
                flagAndAlert("yaw=" + yaw + ", pitch=" + pitch + ", streak=" + streak);
            }
        } else {
            streak = Math.max(0, streak - 1);
            reward();
        }
    }
}
