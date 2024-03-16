package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimConstant")
public class AimConstant extends Check implements RotationCheck {
    public AimConstant(GrimPlayer playerData) {
        super(playerData);
    }


    private double buffer = 0;
    private double decay;
    private int maxBuffer;
    private double minDeltaRot, maxDeltaRotAccel;
    private double lastDeltaX = 0, lastDeltaY = 0;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getDeltaXRot();
        double deltaY = rotationUpdate.getDeltaYRot();
        double deltaRot = Math.hypot(deltaX, deltaY);
        double lastDeltaRot = Math.hypot(lastDeltaX, lastDeltaY);
        double deltaRotAccel = Math.abs(deltaRot - lastDeltaRot);

        //alert("dRA=" + deltaRotAccel + " dA=" + deltaAccel + " diff=" + Math.abs(deltaRotAccel - deltaAccel));
        if(player.compensatedEntities.getSelf().getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if(!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return; //Ignore 90 and -90 pitch rotations
        }
        if(player.packetStateData.lastPacketWasTeleport) {
            return;
        }

        if (deltaRotAccel < maxDeltaRotAccel && deltaRot > minDeltaRot ) {
            if (buffer++ > maxBuffer) {
                flagAndAlert(formatOffset(deltaRotAccel));

            }
        } else {
            buffer = Math.max(0, buffer -decay);
            if(buffer == 0) {
                reward();
            }
        }
        lastDeltaX = deltaX;
        lastDeltaY = deltaY;


    }

    @Override
    public void reload() {
        super.reload();
        maxBuffer = getConfig().getIntElse(getConfigName() + ".buffer", 7);
        decay = getConfig().getDoubleElse(getConfigName() + ".decay", 0.3);
        minDeltaRot = getConfig().getDoubleElse(getConfigName() + ".minDeltaRot", 0.4D);
        maxDeltaRotAccel = getConfig().getDoubleElse(getConfigName() + ".maxDeltaRotAccel", 0.1D);
    }
}
