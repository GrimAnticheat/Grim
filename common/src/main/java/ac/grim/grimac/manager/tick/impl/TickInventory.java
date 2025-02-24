package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.player.GrimPlayer;

public class TickInventory implements Tickable {
    @Override
    public void tick() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.getInventory().inventory.getInventoryStorage().tickWithBukkit();
        }
    }
}
