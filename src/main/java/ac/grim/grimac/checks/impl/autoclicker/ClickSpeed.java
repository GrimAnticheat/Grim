package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.AirSwingCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 */
@CheckData(name = "ClickSpeed", decay = .25, experimental = true)
public class ClickSpeed extends AirSwingCheck {

    public ClickSpeed(GrimPlayer playerData) {
        super(playerData, 50, false, true);
    }

    @Override
    public void handle(Queue<Integer> samples) {
        double cps = GrimMath.getCps(samples);

        if (cps > 50) {
            flagAndAlert("CPS: " + cps);
        } else {
            reward();
        }
    }
}
