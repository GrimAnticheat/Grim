package org.abyssmc.reaperac.events.bukkit;

import org.abyssmc.reaperac.ReaperAC;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;

public class UseFireworkEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().isGliding()) return;
        if (event.getItem().getType() != Material.FIREWORK_ROCKET) return;
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        FireworkMeta fireworkMeta = (FireworkMeta) event.getItem().getItemMeta();

        // Hacked clients could get 11 extra ticks per rocket
        ReaperAC.playerGrimHashMap.get(event.getPlayer()).fireworkElytraDuration = 10 + fireworkMeta.getPower() * 10 + 11;
    }
}
