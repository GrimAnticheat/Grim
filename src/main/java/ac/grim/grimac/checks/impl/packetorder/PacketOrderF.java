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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderF", experimental = true)
public class PacketOrderF extends Check implements PostPredictionCheck {
    public PacketOrderF(final GrimPlayer player) {
        super(player);
    }

    private int invalid = 0;
    private boolean sent = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (sent) {
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                    if (flagAndAlert() && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    invalid++;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();

            if (action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING
                    || action == WrapperPlayClientEntityAction.Action.START_SPRINTING
                    || action == WrapperPlayClientEntityAction.Action.STOP_SNEAKING
                    || action == WrapperPlayClientEntityAction.Action.START_SNEAKING
            ) {
                sent = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (action == DiggingAction.START_DIGGING || action == DiggingAction.CANCELLED_DIGGING || action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.RELEASE_USE_ITEM) {
                sent = true;
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            sent = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // we don't need to check pre-1.9 players here (no tick skipping)
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        if (!player.skippedTickInActualMovement) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
        sent = false;
    }
}
