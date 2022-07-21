package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            if (GrimAPI.INSTANCE.getPlayerDataManager().shouldCheck(event.getUser())) {
                GrimAPI.INSTANCE.getPlayerDataManager().addPlayer(event.getUser(), new GrimPlayer(event.getUser()));
            }
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Player player = (Player) event.getPlayer();
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }
        if (player.hasPermission("grim.alerts") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.enable-on-join", true)) {
            GrimAPI.INSTANCE.getAlertManager().toggleAlerts(player);
        }
        if (player.hasPermission("grim.spectate") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("spectators.hide-regardless", false)) {
            GrimAPI.INSTANCE.getSpectateManager().onLogin(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());
        //Check if calling async is safe
        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            GrimAPI.INSTANCE.getAlertManager().handlePlayerQuit(player);
            GrimAPI.INSTANCE.getSpectateManager().onQuit(player);
        }
    }
}
