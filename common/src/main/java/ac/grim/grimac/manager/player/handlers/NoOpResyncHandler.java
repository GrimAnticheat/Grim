package ac.grim.grimac.manager.player.handlers;

import ac.grim.grimac.api.handler.ResyncHandler;

public enum NoOpResyncHandler implements ResyncHandler {
    INSTANCE;

    @Override
    public void resync(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {}

    @Override public void resyncPosition(int x, int y, int z, int sequence) {}
}
