package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.AirSwingCheck;
import ac.grim.grimac.player.GrimPlayer;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 */
@CheckData(name = "ClickOutliers", decay = .25, experimental = true)
public class ClickOutliers extends AirSwingCheck {

    public ClickOutliers(GrimPlayer playerData) {
        super(playerData, 750, false, true);
    }

    @Override
    public void handle(Queue<Integer> samples) {
        long doubleClicks = samples.stream().filter(integer -> integer == 0).count();
        long outliers = samples.stream().filter(integer -> integer >= 3).count();

        if (outliers < 5) {
            flagAndAlert("Outliers: " + outliers + " Double Clicks: " + doubleClicks);
        } else {
            reward();
        }
    }
}
