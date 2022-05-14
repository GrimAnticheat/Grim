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
    private String brand = "vanilla";
    private boolean hasBrand = false;
    private String message;

    @Override
    public void reload() {
        super.reload();
        message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("client-brand-format", "%prefix% &f%player% joined using %brand%");
    }

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

                brand = new String(minusLength).replace(" (Velocity)", ""); //removes velocity's brand suffix

                if (!hasBrand) {
                    hasBrand = true;
                    if (!GrimAPI.INSTANCE.getConfigManager().isIgnoredClient(brand)) {
                        String msg = MessageUtil.format(message);
                        msg = msg.replace("%brand%", brand);
                        msg = msg.replace("%player%", player.user.getProfile().getName());

                        // sendMessage is async safe while broadcast isn't due to adventure
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("grim.brand")) {
                                player.sendMessage(msg);
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
