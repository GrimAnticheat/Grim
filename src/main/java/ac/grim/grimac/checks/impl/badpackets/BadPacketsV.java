package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;

@CheckData(name = "BadPacketsV")
public class BadPacketsV extends Check implements PacketCheck {
    // Block position that the player digging
    Vector3i targetBlock = null;

    // Block's type or respawn shouldn't make this check false

    public BadPacketsV(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);

            if (digging.getAction() == DiggingAction.START_DIGGING) {
                targetBlock = digging.getBlockPosition();
            }

            if (digging.getAction() == DiggingAction.FINISHED_DIGGING) {
                if (targetBlock == null || !targetBlock.equals(digging.getBlockPosition())) {
                    if (flagAndAlert("start: "+targetBlock+" finished: "+digging.getBlockPosition()) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();

                        // Ends the client prediction introduced in 1.19+
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) {
                            player.user.sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(digging.getSequence()));
                        } else { // The client isn't smart enough to revert changes
                            ResyncWorldUtil.resyncPosition(player, digging.getBlockPosition());
                        }

                    }
                }
            }

        }
    }

}
