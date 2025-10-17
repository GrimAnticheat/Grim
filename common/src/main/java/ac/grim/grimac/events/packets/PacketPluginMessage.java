package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.reflection.ViaVersionUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.kyori.adventure.text.Component.text;

// Grim has no way of knowing the actual protocol version of players when viaversion is used on the proxy
// Luckily, viaversion now forwards the original client version in vv:proxy_details plugin message
// Plugin messages should never be trusted as they can be spoofed, but in this instance, it doesn't matter
// Additionally, viaversion on proxy, when installed, blocks the client from sending any spoofed "vv:proxy_details" packets
// https://github.com/ViaVersion/ViaVersion/blob/fd5dadbe01b4e522def2b0509ef6e831c1ce881d/velocity/src/main/java/com/viaversion/viaversion/velocity/listeners/ConnectionDetailsListener.java#L37
public class PacketPluginMessage extends PacketListenerAbstract {
    private static final String VIA_VERSION_PROXY_DETAILS_CHANNEL = "vv:proxy_details";
    private static final Gson GSON = new GsonBuilder().create();

    public PacketPluginMessage() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handle(event, packet.getChannelName(), packet.getData());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handle(event, packet.getChannelName(), packet.getData());
        }
    }

    private void handle(PacketReceiveEvent event, String channel, byte[] data) {
        if (!channel.equals(VIA_VERSION_PROXY_DETAILS_CHANNEL)) return;

        // Ignore via:proxy messages if we have viaversion locally or aren't using proxy
        if (ViaVersionUtil.isAvailable() || !ProxyAlertMessenger.usingProxy) return;

        if (data.length > 4096) return; // sanity

        String payload = new String(data, StandardCharsets.UTF_8);

        ClientVersion version = null;
        try {
            JsonObject jsonObject = GSON.fromJson(payload, JsonObject.class);
            if (jsonObject.has("version")) { // get client protocol version number
                int versionNumber = jsonObject.get("version").getAsInt();
                version = ClientVersion.getById(versionNumber);
            }
        } catch (Exception ignored) {
        }

        if (version == null) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null || player.hasSentViaProxyPacket) return;

        player.hasSentViaProxyPacket = true;
        player.user.setClientVersion(version);

        if (player.hasPermission("grim.alerts")) {
            player.sendMessage(text("ViaVersion on the proxy has been detected, this may cause issues with 1.9+ clients on 1.8 servers, we recommend installing it on the server itself!", NamedTextColor.RED));
        }
    }
}
