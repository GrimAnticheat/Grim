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

@CheckData(name = "PacketOrderE", experimental = true)
public class PacketOrderE extends Check implements PostPredictionCheck {
    public PacketOrderE(final GrimPlayer player) {
        super(player);
    }

    private int invalidSlots = 0;
    private boolean sent = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (sent) {
                if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) || flagAndAlert()) {
                    invalidSlots++;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION
        ) {
            sent = true;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (action != DiggingAction.CANCELLED_DIGGING && action != DiggingAction.START_DIGGING && action != DiggingAction.FINISHED_DIGGING) {
                sent = true;
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            sent = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            if (invalidSlots > 0) {
                setbackIfAboveSetbackVL();
            }

            invalidSlots = 0;
            return;
        }

        if (!player.skippedTickInActualMovement) {
            for (; invalidSlots >= 1; invalidSlots--) {
                flagAndAlert();
            }
        }

        invalidSlots = 0;
        sent = false;
    }
}
