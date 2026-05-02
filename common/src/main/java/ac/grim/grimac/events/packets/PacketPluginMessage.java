package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import org.jetbrains.annotations.NotNull;

public class PacketPluginMessage extends PacketListenerAbstract {

    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            checkChannel(event.getUser(), packet.getChannelName());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            checkChannel(event.getUser(), packet.getChannelName());
        }
    }

    private void checkChannel(User user, String channelName) {
        if (!"vv:proxy_details".equals(channelName)) return;
        final boolean usingProxy = ProxyAlertMessenger.isUsingProxy();
        // warn if they are using a proxy
        if (usingProxy) {
            LogUtil.warn(
                    user.getName() + " seems to have connected through a proxy running ViaVersion. "
                            + "Having ViaVersion installed on the proxy is incompatible with GrimAC and causes many issues. "
                            + "Please remove ViaVersion from your proxy server and install it on your backend servers instead."
            );
        }
        // kick if they do not have a proxy configured OR they have ViaVersion installed on the backend
        if (CommonGrimArguments.KICK_ON_VIA_PROXY.value() && (!usingProxy || ViaVersionUtil.isAvailable)) {

            LogUtil.warn(user.getName() + " is being disconnected for sending ViaVersion proxy data.");

            try {
                WrapperPlayServerDisconnect disconnect = new WrapperPlayServerDisconnect(
                        MessageUtil.miniMessage(GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())
                );
                user.sendPacket(disconnect);
            } catch (Exception e) {
                LogUtil.warn("Failed to send disconnect packet to kick " + user.getName() + "!");
            }
            user.closeConnection();
        }
    }

}
