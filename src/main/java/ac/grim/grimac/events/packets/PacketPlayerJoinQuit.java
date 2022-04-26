package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {
    @Override
    public void onUserConnect(UserConnectEvent event) {
        new GrimPlayer(event.getUser()); // Player takes care of adding to hashmap
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = (Player) event.getPlayer();
        if (player.hasPermission("grim.alerts") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.enable-on-join", true)) {
            GrimAPI.INSTANCE.getAlertManager().toggle(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());

        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            GrimAPI.INSTANCE.getAlertManager().handlePlayerQuit(player);
        }
    }
}
