package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.ArmAnimationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.ArmAnimationUpdate;

/*
 Information in regard to why this was added can be found here:
 https://github.com/GrimAnticheat/Grim/pull/1631
 */
@CheckData(name = "AutoClickerA", alternativeName = "AutoclickerA")
public class AutoClickerA extends Check implements ArmAnimationCheck {
    private int MAX_CPS;

    public AutoClickerA(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final ArmAnimationUpdate armAnimationUpdate) {
        final int cps = armAnimationUpdate.getLeftClicks();
        if (cps > MAX_CPS) alert("cps=" + cps + ", max=" + MAX_CPS);
    }

    @Override
    public void reload() {
        super.reload();
        MAX_CPS = getConfig().getIntElse("AutoClicker.max_cps", 20);
    }
}
