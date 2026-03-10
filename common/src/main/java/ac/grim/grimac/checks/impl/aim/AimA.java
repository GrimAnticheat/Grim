package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimA", description = "Very low pitch variance while tracking targets", decay = 0.02, experimental = true)
public class AimA extends Check implements RotationCheck {
    private int streak;

    public AimA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle() || !player.actionManager.hasAttackedSince(250)) {
            streak = 0;
            return;
        }

        final float absYaw = rotationUpdate.getDeltaXRotABS();
        final float absPitch = rotationUpdate.getDeltaYRotABS();

        if (absYaw > 2.0F && absPitch < 0.01F && !rotationUpdate.isCinematic()) {
            if (++streak > 8) {
                flagAndAlert("yaw=" + absYaw + ", pitch=" + absPitch + ", streak=" + streak);
            }
        } else {
            streak = Math.max(0, streak - 1);
            reward();
        }
    }
}
