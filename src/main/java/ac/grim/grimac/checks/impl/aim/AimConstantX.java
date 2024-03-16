package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimConstantX")
public class AimConstantX extends Check implements RotationCheck {
    public AimConstantX(GrimPlayer playerData) {
        super(playerData);
    }


    private double buffer = 0;
    private double decay;
    private int maxBuffer;
    private double minDeltaX, maxDeltaXAccel;

    private double lastDeltaX = 0;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getDeltaXRotABS();

        double deltaXAccel = Math.abs(deltaX - lastDeltaX);
        if(player.compensatedEntities.getSelf().getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if(!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return; //Ignore 90 and -90 pitch rotations
        }
        if(player.packetStateData.lastPacketWasTeleport) {
            return;
        }


        if (deltaXAccel < maxDeltaXAccel && deltaX > minDeltaX) {
            if (buffer++ > maxBuffer) {
                flagAndAlert(formatOffset(deltaXAccel));

            }
        } else {
            buffer = Math.max(0, buffer -decay);
            if(buffer == 0) {
                reward();
            }
        }
        lastDeltaX = deltaX;


    }

    @Override
    public void reload() {
        super.reload();
        maxBuffer = getConfig().getIntElse(getConfigName() + ".buffer", 7);
        decay = getConfig().getDoubleElse(getConfigName() + ".decay", 0.3);
        minDeltaX = getConfig().getDoubleElse(getConfigName() + ".minDeltaX", 0.4D);
        maxDeltaXAccel = getConfig().getDoubleElse(getConfigName() + ".maxDeltaXAccel", 0.1D);
    }
}
