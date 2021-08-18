package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;

public class LastTransactionSetter implements Tickable {
    @Override
    public void tick() {
        GrimAPI.INSTANCE.getPlayerDataManager().getEntries().forEach(player -> player.lastTransactionAtStartOfTick = player.packetStateData.packetLastTransactionReceived.get());
    }
}
