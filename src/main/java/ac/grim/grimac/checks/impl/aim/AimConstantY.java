package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimConstantY")
public class AimConstantY extends Check implements RotationCheck {
    public AimConstantY(GrimPlayer playerData) {
        super(playerData);
    }


    private double buffer = 0;
    private double decay;
    private int maxBuffer;
    private double minDeltaY, maxDeltaYAccel;
    private double lastDeltaY = 0;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaY = rotationUpdate.getDeltaYRotABS();

        double deltaYAccel = Math.abs(deltaY - lastDeltaY);
        if(player.compensatedEntities.getSelf().getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if(!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return; //Ignore 90 and -90 pitch rotations
        }
        if(player.packetStateData.lastPacketWasTeleport) {
            return;
        }

        if (deltaYAccel < maxDeltaYAccel && deltaY > minDeltaY) {
            if (buffer++ > maxBuffer) {
                flagAndAlert(formatOffset(deltaYAccel));

            }
        } else {
            buffer = Math.max(0, buffer -decay);
            if(buffer == 0) {
                reward();
            }
        }
        lastDeltaY = deltaY;


    }

    @Override
    public void reload() {
        super.reload();
        maxBuffer = getConfig().getIntElse(getConfigName() + ".buffer", 7);
        decay = getConfig().getDoubleElse(getConfigName() + ".decay", 0.3);
        minDeltaY = getConfig().getDoubleElse(getConfigName() + ".minDeltaY", 0.4D);
        maxDeltaYAccel = getConfig().getDoubleElse(getConfigName() + ".maxDeltaYAccel", 0.1D);
    }
}
