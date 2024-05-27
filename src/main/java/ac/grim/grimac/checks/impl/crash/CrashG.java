package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

@CheckData(name = "CrashG")
public class CrashG extends Check implements PacketCheck {

    public CrashG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) &&
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) return;

        if (!isFlagged(event)) return;

        flagAndAlert();
        event.setCancelled(true);
        player.onPacketCancel();
    }

    private static boolean isFlagged(final PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PLAYER_BLOCK_PLACEMENT) return new WrapperPlayClientPlayerBlockPlacement(event).getSequence() < 0;
        if (packetType == PLAYER_DIGGING) return new WrapperPlayClientPlayerDigging(event).getSequence() < 0;
        if (packetType == USE_ITEM) return new WrapperPlayClientUseItem(event).getSequence() < 0;
        return false;
    }

}
