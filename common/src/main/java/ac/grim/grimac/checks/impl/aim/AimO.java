package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimO", description = "Repeated post-snap idle look frames", decay = 0.02, experimental = true)
public class AimO extends Check implements RotationCheck {
    private int postSnapIdle;
    private boolean hadSnap;

    public AimO(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(350) || player.packetStateData.lastPacketWasTeleport || player.inVehicle()) {
            postSnapIdle = 0;
            hadSnap = false;
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        if (yaw > 30.0F) {
            hadSnap = true;
            return;
        }

        if (hadSnap && yaw < 0.02F) {
            if (++postSnapIdle > 7) {
                flagAndAlert("idle=" + postSnapIdle + ", yaw=" + yaw);
            }
        } else {
            postSnapIdle = Math.max(0, postSnapIdle - 1);
            reward();
        }

        hadSnap = false;
    }
}
