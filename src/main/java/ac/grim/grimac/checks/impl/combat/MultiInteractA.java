package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.ArrayList;

@CheckData(name = "MultiInteractA", description = "Interacted with multiple entities in the same tick", experimental = true, checkType = CheckType.COMBAT)
public class MultiInteractA extends Check implements PostPredictionCheck {
    public MultiInteractA(final GrimPlayer player) {
        super(player);
    }

    private final ArrayList<String> flags = new ArrayList<>();
    private int lastEntity;
    private boolean lastSneaking;
    private boolean hasInteracted = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            int entity = packet.getEntityId();
            boolean sneaking = packet.isSneaking().orElse(false);

            if (hasInteracted && entity != lastEntity) {
                String verbose = "lastEntity=" + lastEntity + ", entity=" + entity
                        + ", lastSneaking=" + lastSneaking + ", sneaking=" + sneaking;
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }

            lastEntity = entity;
            lastSneaking = sneaking;
            hasInteracted = true;
        }

        if (player.gamemode == GameMode.SPECTATOR || isTickPacket(event.getPacketType())) {
            hasInteracted = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
