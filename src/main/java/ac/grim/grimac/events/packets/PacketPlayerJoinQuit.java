package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {
    @Override
    public void onUserConnect(UserConnectEvent event) {
        new GrimPlayer(event.getUser()); // Player takes care of adding to hashmap
        LogUtil.info("Connection initialized, total players connected: " + GrimAPI.INSTANCE.getPlayerDataManager().size());
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = (Player) event.getPlayer();
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }
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
            GrimAPI.INSTANCE.getSpectateManager().handlePlayerStopSpectating(player.getUniqueId());
        }
    }
}
