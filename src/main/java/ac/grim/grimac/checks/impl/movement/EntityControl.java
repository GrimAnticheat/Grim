package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Entity control", configName = "EntityControl", alertInterval = 40, dontAlertUntil = 40)
public class EntityControl extends PostPredictionCheck {
    public EntityControl(GrimPlayer player) {
        super(player);
    }

    public void flag() {
        increaseViolations();
    }

    public void rewardPlayer() {
        reward();
    }
}
