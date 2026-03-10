package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;

// Intended for future events we inject all platforms at the end of a tick
public abstract class AbstractTickEndEvent implements StartableInitable {

    @Override
    public void start() {

    }

    protected void onEndOfTick(GrimPlayer player) {
        player.checkManager.getEntityReplication().onEndOfTickEvent();
    }

    protected boolean shouldInjectEndTick() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("Reach.enable-post-packet", false);
    }
}
