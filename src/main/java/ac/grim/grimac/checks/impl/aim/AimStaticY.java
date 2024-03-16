package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimStaticX")
public class AimStaticY extends Check implements RotationCheck {
    public AimStaticY(GrimPlayer playerData) {
        super(playerData);
    }


    private double buffer = 0;
    private double decay;
    private int maxBuffer;
    private double minDeltaX, maxDeltaY;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getDeltaXRotABS();
        double deltaY = rotationUpdate.getDeltaYRotABS();
        if(player.compensatedEntities.getSelf().getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if(!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return; //Ignore 90 and -90 pitch rotations
        }
        if(player.packetStateData.lastPacketWasTeleport) {
            return;
        }
        if (deltaY < maxDeltaY && deltaX > minDeltaX) {
            if (buffer++ > maxBuffer) {
                flagAndAlert(formatOffset(deltaY));

            }
        } else {
            buffer = Math.max(0, buffer -decay);
            if(buffer == 0) {
                reward();
            }
        }


    }

    @Override
    public void reload() {
        super.reload();
        maxBuffer = getConfig().getIntElse(getConfigName() + ".buffer", 7);
        decay = getConfig().getDoubleElse(getConfigName() + ".decay", 1);
        minDeltaX = getConfig().getDoubleElse(getConfigName() + ".minDeltaX", 1D);
        maxDeltaY = getConfig().getDoubleElse(getConfigName() + ".maxDeltaX", 0.0001D);
    }
}
