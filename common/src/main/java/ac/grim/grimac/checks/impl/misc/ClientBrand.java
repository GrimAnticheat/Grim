package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import ac.grim.grimac.utils.chat.ChatUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;
import net.kyori.adventure.text.Component;

public class ClientBrand extends Check implements PacketCheck {
    public static final String channel = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) ? "minecraft:brand" : "MC|Brand";

    @Getter
    private String brand = "vanilla";
    private boolean hasBrand = false;

    public ClientBrand(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        }
    }


    private void handle(String channel, byte[] data) {
        if (!channel.equals(ClientBrand.channel)) return;

        if (data.length > 64 || data.length == 0) {
            brand = "sent " + data.length + " bytes as brand";
        } else if (!hasBrand) {
            byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);

            brand = new String(minusLength).replace(" (Velocity)", ""); // removes velocity's brand suffix
            brand = ChatUtil.stripColor(brand); // strip color codes from client brand
            if (!GrimAPI.INSTANCE.getConfigManager().isIgnoredClient(brand)) {
                String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("client-brand-format", "%prefix% &f%player% joined using %brand%");
                Component component = MessageUtil.replacePlaceholders(player, MessageUtil.miniMessage(message));

                GrimAPI.INSTANCE.getAlertManager().sendBrand(component);
            }
        }

        hasBrand = true;
    }
}
