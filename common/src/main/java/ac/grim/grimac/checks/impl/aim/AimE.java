package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimE", description = "Snap-and-stop aiming pattern", decay = 0.02, experimental = true)
public class AimE extends Check implements RotationCheck {
    private boolean snappedLastTick;
    private int snapStopStreak;

    public AimE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float absYaw = rotationUpdate.getDeltaXRotABS();

        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle() || !player.actionManager.hasAttackedSince(300)) {
            snappedLastTick = false;
            snapStopStreak = 0;
            return;
        }

        if (snappedLastTick && absYaw < 0.05F) {
            if (++snapStopStreak > 6) {
                flagAndAlert("streak=" + snapStopStreak + ", yaw=" + absYaw);
            }
        } else if (absYaw > 35.0F) {
            snappedLastTick = true;
            reward();
            return;
        } else {
            snapStopStreak = Math.max(0, snapStopStreak - 1);
            reward();
        }

        snappedLastTick = false;
    }
}
