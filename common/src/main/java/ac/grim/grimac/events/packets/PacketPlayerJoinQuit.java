package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.event.events.GrimQuitEvent;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> GrimAPI.INSTANCE.getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        // Player connected too soon, perhaps late bind is off
        // Don't kick everyone on reload
        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.contains(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Object nativePlayerObject = event.getPlayer();
        Preconditions.checkArgument(nativePlayerObject != null);

        // This will never throw a NPE because code is run in OnUserConnect -> onPacketSend -> OnUserLogin order
        // And the user will be added to the map before the getPlayer() method call
        @NonNull PlatformPlayer platformPlayer = GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(nativePlayerObject);

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }
        if (platformPlayer.hasPermission("grim.alerts.enable-on-join") && platformPlayer.hasPermission("grim.alerts")) {
            GrimAPI.INSTANCE.getAlertManager().toggleAlerts(platformPlayer, platformPlayer.hasPermission("grim.alerts.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("grim.verbose.enable-on-join") && platformPlayer.hasPermission("grim.verbose")) {
            GrimAPI.INSTANCE.getAlertManager().toggleVerbose(platformPlayer, platformPlayer.hasPermission("grim.verbose.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("grim.brand.enable-on-join") && platformPlayer.hasPermission("grim.brand")) {
            GrimAPI.INSTANCE.getAlertManager().toggleBrands(platformPlayer, platformPlayer.hasPermission("grim.brand.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("grim.spectate") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("spectators.hide-regardless", false)) {
            GrimAPI.INSTANCE.getSpectateManager().onLogin(platformPlayer.getUniqueId());
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());
        if (grimPlayer != null) GrimAPI.INSTANCE.getEventBus().post(new GrimQuitEvent(grimPlayer));
        GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.remove(event.getUser());

        UUID uuid = event.getUser().getProfile().getUUID();

        //Check if calling async is safe
        if (uuid == null)
            return; // folia doesn't like null getPlayer()

        GrimAPI.INSTANCE.getAlertManager().handlePlayerQuit(
                GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(uuid)
        );

        GrimAPI.INSTANCE.getSpectateManager().onQuit(uuid);

        // TODO (Cross-platform) confirm this is 100% correct and will always remove players from cache when necessary
        GrimAPI.INSTANCE.getPlatformPlayerFactory().invalidatePlayer(uuid);
    }
}
