package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimJ", description = "Excessive yaw acceleration spikes", decay = 0.02, experimental = true)
public class AimJ extends Check implements RotationCheck {
    private float lastYaw;
    private int spikes;

    public AimJ(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        float yaw = rotationUpdate.getDeltaXRotABS();
        if (!player.actionManager.hasAttackedSince(300) || player.packetStateData.lastPacketWasTeleport) {
            lastYaw = yaw;
            spikes = 0;
            return;
        }

        float accel = Math.abs(yaw - lastYaw);
        if (yaw > 8.0F && accel > 14.0F) {
            if (++spikes > 6) {
                flagAndAlert("yaw=" + yaw + ", accel=" + accel + ", spikes=" + spikes);
            }
        } else {
            spikes = Math.max(0, spikes - 1);
            reward();
        }

        lastYaw = yaw;
    }
}
