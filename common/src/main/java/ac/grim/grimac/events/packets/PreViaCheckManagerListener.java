package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

public class PreViaCheckManagerListener extends PacketListenerAbstract {
    public PreViaCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            player.serverOpenedInventoryThisTick = false;
            if (!player.packetStateData.didSendMovementBeforeTickEnd) {
                // The player didn't send a movement packet, so we can predict this like we had idle tick on 1.8
                player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
                player.packetStateData.didLastMovementIncludePosition = false;
            }
            player.packetStateData.didSendMovementBeforeTickEnd = false;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketSend(event);
    }
}
