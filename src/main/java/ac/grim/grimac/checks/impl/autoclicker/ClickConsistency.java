package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.AirSwingCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 */
@CheckData(name = "ClickConsistency", decay = .25, experimental = true)
public class ClickConsistency extends AirSwingCheck {

    public ClickConsistency(GrimPlayer playerData) {
        super(playerData, 500, false, true);
    }

    @Override
    public void handle(Queue<Integer> samples) {
        double stDev = GrimMath.getStandardDeviation(samples);

        if (stDev < 0.4) {
            flagAndAlert("STD: " + stDev);
        } else {
            reward();
        }
    }
}
