package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.AirSwingCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 */
@CheckData(name = "ClickNegativeKurtosis", decay = .25, experimental = true)
public class ClickNegativeKurtosis extends AirSwingCheck {

    public ClickNegativeKurtosis(GrimPlayer playerData) {
        super(playerData, 500, true, true);
    }

    @Override
    public void handle(Queue<Integer> samples) {
        double kurtosis = GrimMath.getKurtosis(samples);

        if (kurtosis < 0D) {
            flagAndAlert("Kurtosis: " + kurtosis);
        } else {
            reward();
        }
    }
}
