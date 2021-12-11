package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import org.bukkit.Bukkit;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            Bukkit.broadcastMessage(((CompensatedInventory) player.checkManager.getPacketCheck(CompensatedInventory.class)).getHeldItem().toString());
            if (player.getClientVersion().getProtocolVersion() == -1) player.pollClientVersion();
        }
    }
}
