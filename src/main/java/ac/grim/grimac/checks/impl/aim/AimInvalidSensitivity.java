package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimInvalidSensitivity")
public class AimInvalidSensitivity extends Check implements RotationCheck {
    public AimInvalidSensitivity(GrimPlayer playerData) {
        super(playerData);
    }

    int xRotsSinceModeChange = 0, yRotsSinceModeChange = 0;
    int maxRots;


    @Override
    public void process(final RotationUpdate rotationUpdate) {

        int sensitivityX = (int) Math.round(player.getHorizontalSensitivity() * 200);
        int sensitivityY = (int) Math.round(player.getVerticalSensitivity() * 200);

        if(sensitivityX != sensitivityY && sensitivityX > 5 && sensitivityY > 5) {

            if (rotationUpdate.getDeltaXRotABS() > 0 && rotationUpdate.getDeltaXRotABS() < 5 && rotationUpdate.getProcessor().divisorX > GrimMath.MINIMUM_DIVISOR) {
                xRotsSinceModeChange++;
            }
            if (rotationUpdate.getDeltaYRotABS() > 0 && rotationUpdate.getDeltaYRotABS() < 5 && rotationUpdate.getProcessor().divisorY > GrimMath.MINIMUM_DIVISOR) {
                yRotsSinceModeChange++;
            }

            //Need some buffer bc the player can change sensitivity
            if (xRotsSinceModeChange > maxRots && yRotsSinceModeChange > maxRots) {
                flagAndAlert("sensitivityX=" + sensitivityX + " sensitivityY=" + sensitivityY);
            }


        }

    }

    @Override
    public void reload() {
        super.reload();
        maxRots = getConfig().getIntElse(getConfigName() + ".maxRots", 80);

    }
}
