package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.player.GrimPlayer;

public class SendTransaction implements Tickable {
    @Override
    public void tick() {
        // Writing packets takes more time than it appears - don't flush to try and get the packet to send right before
        // the server begins sending packets to the client
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.sendTransactionOrPingPong(player.getNextTransactionID(1), true);
        }
    }
}
