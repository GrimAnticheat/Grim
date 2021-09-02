package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Entity control", buffer = 10, maxBuffer = 15)
public class EntityControl extends PostPredictionCheck {
    public EntityControl(GrimPlayer player) {
        super(player);
    }

    // We don't alert on this check because we don't have lag compensated inventories.
    // TODO: Add latency compensated inventories
    public boolean flag() {
        decreaseBuffer(1);

        return getBuffer() == 0;
    }

    public void rewardPlayer() {
        increaseBuffer();
    }
}
