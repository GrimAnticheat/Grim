package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.movement.NoSlow;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsM")
public class BadPacketsM extends Check implements PostPredictionCheck {
    boolean sentHeldItem = false;
    boolean check = false;

    public BadPacketsM(GrimPlayer playerData) {
        super(playerData);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (check && player.isTickingReliablyFor(3)) {
            if (flagAndAlert()) {
                player.checkManager.getPostPredictionCheck(NoSlow.class).flagWithSetback(); // Impossible to false, call NoSlow violation to setback
            }
        }
        check = false;
    }

    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) return;
        // Due to a bug in 1.8 clients, this check isn't possible for 1.8 clients
        // Instead, we must tick "using item" with flying packets like the server does
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) { // idle packet
            if (sentHeldItem) {
                check = true;
            } else {
                sentHeldItem = true;
            }
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            sentHeldItem = false;
        }
    }

}
