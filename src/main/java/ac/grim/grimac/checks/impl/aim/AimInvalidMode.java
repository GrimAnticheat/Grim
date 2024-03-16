package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimInvalidMode")
public class AimInvalidMode extends Check implements RotationCheck {
    public AimInvalidMode(GrimPlayer playerData) {
        super(playerData);
    }

    double lastModeX = 0;
    double lastModeY = 0;
    int xRotsSinceModeChange = 0, yRotsSinceModeChange = 0;
    int maxRots;


    @Override
    public void process(final RotationUpdate rotationUpdate) {

        double modeX = rotationUpdate.getProcessor().modeX;
        double modeY = rotationUpdate.getProcessor().modeY;


        if (((modeX == lastModeX) != (modeY == lastModeY)) && (lastModeX != 0 && lastModeY != 0 && lastModeX < 1 && lastModeY < 1)) {
            if (rotationUpdate.getDeltaXRotABS() > 0 && rotationUpdate.getDeltaXRotABS() < 5 && rotationUpdate.getProcessor().divisorX > GrimMath.MINIMUM_DIVISOR) {
                xRotsSinceModeChange++;
            }
            if (rotationUpdate.getDeltaYRotABS() > 0 && rotationUpdate.getDeltaYRotABS() < 5 && rotationUpdate.getProcessor().divisorY > GrimMath.MINIMUM_DIVISOR) {
                yRotsSinceModeChange++;
            }

            //Need some buffer bc the player can change sensitivity
            if (xRotsSinceModeChange > maxRots && yRotsSinceModeChange > maxRots) {
                flagAndAlert("modeX=" + modeX + " lmodeX=" + lastModeX + " modeY=" + modeY + " lmodeY=" + lastModeY);
            }

            return; //return here so the check keeps flagging till both modes change
        } else {
            xRotsSinceModeChange = yRotsSinceModeChange = 0;
        }
        lastModeX = modeX;
        lastModeY = modeY;


    }

    @Override
    public void reload() {
        super.reload();
        maxRots = getConfig().getIntElse(getConfigName() + ".maxRots", 80);

    }
}
