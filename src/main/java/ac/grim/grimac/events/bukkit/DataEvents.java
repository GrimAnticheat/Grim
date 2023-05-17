package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class DataEvents implements Listener {
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (grimPlayer == null) return;
        grimPlayer.lastSendedCommand = event.getMessage();
    }

    @EventHandler
    public void onAction(PlayerInteractEvent event) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (grimPlayer == null) return;
        grimPlayer.lastInteractAction = event.getAction() +
                (event.getClickedBlock() == null ? "" : "_" + event.getClickedBlock().getType());
    }

    @EventHandler
    public void onAction(PlayerInteractAtEntityEvent event) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (grimPlayer == null) return;
        grimPlayer.lastInteractAction = "ENTITY_" + event.getRightClicked().getType() + "_INTERACT";
    }
}
