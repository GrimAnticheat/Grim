package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.GrimAlerts;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {
    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = (Player) event.getPlayer();
        if (player.hasPermission("grim.alerts")) {
            if (GrimAPI.INSTANCE.getPlugin().getConfig().getBoolean("alerts.enable-on-join")) {
                String alertString = GrimAPI.INSTANCE.getPlugin().getConfig().getString("messages.alerts-enabled", "%prefix% &fAlerts &benabled");
                player.sendMessage(MessageUtil.format(alertString));
            } else {
                GrimAlerts.toggle(player);
            }
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());
        GrimAlerts.handlePlayerQuit(Bukkit.getPlayer(event.getUser().getProfile().getUUID()));
    }
}
