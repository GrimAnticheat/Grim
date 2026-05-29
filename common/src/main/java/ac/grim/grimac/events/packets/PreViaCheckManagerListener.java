package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import org.jetbrains.annotations.NotNull;

public class PreViaCheckManagerListener extends PacketListenerAbstract {

    public PreViaCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(final @NotNull PacketReceiveEvent event) {
        // Allow checks to listen to configuration packets
        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.checkManager.onPreViaPacketReceive(event);
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketReceive(event);
    }

    @Override
    public void onPacketSend(final @NotNull PacketSendEvent event) {
        // Allow checks to listen to configuration packets
        if (event.getConnectionState() == ConnectionState.CONFIGURATION) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.checkManager.onPreViaPacketSend(event);
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketSend(event);
    }
}
