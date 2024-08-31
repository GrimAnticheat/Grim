package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayDeque;
import java.util.ArrayList;

@CheckData(name = "PacketOrderK", experimental = true)
public class PacketOrderK extends Check implements PostPredictionCheck {
    public PacketOrderK(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<String> flags = new ArrayDeque<>();
    private int lastEntity;
    private boolean hasInteracted = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            int entity = new WrapperPlayClientInteractEntity(event).getEntityId();
            if (hasInteracted && entity != lastEntity) {
                String verbose = "lastEntity=" + lastEntity + ", entity=" + entity;
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }
            lastEntity = entity;
            hasInteracted = true;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            hasInteracted = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // we don't need to check pre-1.9 players here (no tick skipping)
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        if (!player.skippedTickInActualMovement) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
        hasInteracted = false;
    }
}
