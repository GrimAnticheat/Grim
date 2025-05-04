package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

@CheckData(name = "BadPacketsH", description = "Sent unexpected sequence id", experimental = true)
public class BadPacketsH extends Check implements PacketCheck {
    private int lastSequence;
    private final boolean isSupportedVersion = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);

    public BadPacketsH(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isSupportedVersion) {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                handleSequenceId(new WrapperPlayClientPlayerBlockPlacement(event).getSequence(), event);
            }

            if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
                if (packet.getAction() == DiggingAction.START_DIGGING || packet.getAction() == DiggingAction.FINISHED_DIGGING) {
                    handleSequenceId(packet.getSequence(), event);
                } else if (packet.getSequence() != 0 && packet.getAction() == DiggingAction.CANCELLED_DIGGING
                        && flagAndAlert("expected=0, id=" + packet.getSequence()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }

            if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
                handleSequenceId(new WrapperPlayClientUseItem(event).getSequence(), event);
            }
        }
    }

    public void handleSequenceId(int sequence, PacketReceiveEvent event) {
        if (sequence != lastSequence + 1) {
            if (flagAndAlert("expected=" + (lastSequence + 1) + ", id=" + sequence) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        lastSequence = sequence;
    }

    public void onWorldChange() {
        lastSequence = 0;
    }
}
