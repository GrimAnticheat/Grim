package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ClientBrand extends PacketCheck {
    String brand = "vanilla";
    boolean hasBrand = false;

    public ClientBrand(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

            if (packet.getChannelName().equalsIgnoreCase("minecraft:brand") || // 1.13+
                    packet.getChannelName().equals("MC|Brand")) { // 1.12

                byte[] data = packet.getData();

                if (data.length == 0) {
                    brand = "received empty brand";
                    return;
                }

                byte[] minusLength = new byte[data.length - 1];
                System.arraycopy(data, 1, minusLength, 0, minusLength.length);

                brand = new String(minusLength);

                if (!hasBrand) {
                    hasBrand = true;

                    if (!GrimAPI.INSTANCE.getPlugin().getConfig().getStringList("client-brand.ignored-clients").contains(brand)) {
                        String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("client-brand-format", "%prefix% &f%player% joined using %brand%");
                        message = MessageUtil.format(message);
                        message = message.replace("%brand%", brand);
                        message = message.replace("%player%", player.user.getProfile().getName());

                        // sendMessage is async safe while broadcast isn't due to adventure
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("grim.brand")) {
                                player.sendMessage(message);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getBrand() {
        return brand;
    }
}
