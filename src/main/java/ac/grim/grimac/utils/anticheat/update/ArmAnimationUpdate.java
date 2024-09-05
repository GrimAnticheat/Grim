package ac.grim.grimac.utils.anticheat.update;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArmAnimationUpdate {
    private int leftClicks, rightClicks;

    public ArmAnimationUpdate(int leftClicks, int rightClicks) {
        this.leftClicks = leftClicks;
        this.rightClicks = rightClicks;
    }
}
