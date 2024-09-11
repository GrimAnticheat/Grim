package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderG", experimental = true)
public class PacketOrderG extends Check implements PostPredictionCheck {
    public PacketOrderG(final GrimPlayer player) {
        super(player);
    }

    private int invalid = 0;
    private boolean sent = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();

            if (action == DiggingAction.START_DIGGING || action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING) {
                sent = true;
                return;
            }

            if (sent) {
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                    if (!flagAndAlert()) {
                        return;
                    }

                    if (shouldModifyPackets() && action != DiggingAction.RELEASE_USE_ITEM) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }

                invalid++;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            sent = true;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            sent = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            if (invalid > 0) {
                setbackIfAboveSetbackVL();
            }

            invalid = 0;
        } else {
            if (!player.skippedTickInActualMovement) {
                for (; invalid > 0; invalid--) {
                    if (flagAndAlert()) {
                        setbackIfAboveSetbackVL();
                    }
                }
            }

            invalid = 0;
            sent = false;
        }
    }
}
