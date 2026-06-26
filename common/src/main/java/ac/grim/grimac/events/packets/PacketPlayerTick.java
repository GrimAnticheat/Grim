package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class PacketPlayerTick extends PacketListenerAbstract {

    public PacketPlayerTick() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2))
                return;

            PacketWorldBorder border = player.checkManager.getPacketCheck(PacketWorldBorder.class);
            border.tickBorder();
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2))
                return;

            PacketWorldBorder border = player.checkManager.getPacketCheck(PacketWorldBorder.class);
            border.tickBorder();
        }
    }

}
