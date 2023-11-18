package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsR", experimental = true)
public class BadPacketsR extends Check implements PostPredictionCheck {
    public BadPacketsR(GrimPlayer player) {
        super(player);
    }

    private boolean hasSentPlace;

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // call onPredictionComplete to get updated value of isTickingReliablyFor
        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) && (!player.skippedTickInActualMovement || !player.isTickingReliablyFor(3))) {
            hasSentPlace = false;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            hasSentPlace = false;
            return;
        }

        if (packetType == Client.PLAYER_BLOCK_PLACEMENT) {
            hasSentPlace = true;
        } else if (packetType == Client.INTERACT_ENTITY) {
            if (hasSentPlace) {
                WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
                if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    if (flagAndAlert() && shouldModifyPackets()) {
                        player.onPacketCancel();
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
