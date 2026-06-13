package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.ArrayList;

@CheckData(name = "MultiInteractB", stableKey = "grim.multiinteract.interact_at_position_changed", verboseVersion = 1, experimental = true)
public class MultiInteractB extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "posX:f64", "posY:f64", "posZ:f64", "lastPosX:f64", "lastPosY:f64", "lastPosZ:f64");

    private final ArrayList<FlagData> flags = new ArrayList<>();
    private Vector3d lastPos;
    private boolean hasInteracted;

    public MultiInteractB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) return;

            Vector3d pos = packet.getLocation();
            if (pos == null) return; // shouldn't ever happen, but whatever

            if (hasInteracted && !pos.equals(lastPos)) {
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose()).f64(pos.x).f64(pos.y).f64(pos.z).f64(lastPos.x).f64(lastPos.y).f64(lastPos.z))
                            && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(pos.x, pos.y, pos.z, lastPos.x, lastPos.y, lastPos.z));
                }
            }

            lastPos = pos;
            hasInteracted = true;
        }

        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasInteracted = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose())
                        .f64(data.posX()).f64(data.posY()).f64(data.posZ())
                        .f64(data.lastPosX()).f64(data.lastPosY()).f64(data.lastPosZ()));
            }
        }

        flags.clear();
    }

    private record FlagData(
            double posX,
            double posY,
            double posZ,
            double lastPosX,
            double lastPosY,
            double lastPosZ) {
    }
}
