package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

public class PacketConfigurationListener extends PacketListenerAbstract {

    public PacketConfigurationListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            //
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            String channelName = wrapper.getChannelName();
            byte[] data = wrapper.getData();
            if (channelName.equalsIgnoreCase("minecraft:brand") || channelName.equals("MC|Brand")) {
                player.checkManager.getPacketCheck(ClientBrand.class).handle(channelName, data);
            }
        }
    }

}
