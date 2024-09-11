package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderL", experimental = true)
public class PacketOrderL extends Check implements PostPredictionCheck {
    public PacketOrderL(final GrimPlayer player) {
        super(player);
    }

    private int invalid = 0;
    private boolean sent = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case START_DIGGING:
                    if (BlockBreakSpeed.getBlockDamage(player, packet.getBlockPosition()) >= 1) {
                        return; // can false on insta-break blocks
                    }
                case CANCELLED_DIGGING:
                case FINISHED_DIGGING:
                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                        return; // valid on 1.7
                    }
                case RELEASE_USE_ITEM:
                    sent = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
            if (action == WrapperPlayClientEntityAction.Action.START_SNEAKING
                    || action == WrapperPlayClientEntityAction.Action.STOP_SNEAKING
                    || action == WrapperPlayClientEntityAction.Action.START_SPRINTING
                    || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                sent = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
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
