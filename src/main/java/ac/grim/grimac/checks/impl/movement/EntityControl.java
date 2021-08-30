package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Entity control")
public class EntityControl extends PostPredictionCheck {
    public EntityControl(GrimPlayer player) {
        super(player);
    }

    public boolean flag() {
        decreaseBuffer(1);
        return getBuffer() == 0;
    }

    public void reward() {
        increaseBuffer(0.25);
    }
}
