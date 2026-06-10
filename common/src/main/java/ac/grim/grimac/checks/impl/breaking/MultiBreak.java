package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", stableKey = "grim.breaking.multi_break", verboseVersion = 2, description = "Tried to break multiple different blocks in the same movement tick", experimental = true)
public class MultiBreak extends Check implements BlockBreakCheck {
    public static final VerboseSchema V = VerboseSchema.of(2,
            "face:enum", "lastFace:enum", "posXZ:vl", "posY:zz", "lastPosXZ:vl", "lastPosY:zz");

    private final List<FlagData> flags = new ArrayList<>();
    private boolean hasBroken;
    private BlockFace lastFace;
    private Vector3i lastPos;

    public MultiBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        if (hasBroken && (blockBreak.face != lastFace || !blockBreak.position.equals(lastPos))) {
            final int face = VerboseCodecs.enumOrdinal(blockBreak.face);
            final int previousFace = VerboseCodecs.enumOrdinal(lastFace);
            if (!player.canSkipTicks()) {
                var buf = V.write(verbose()).vi(face).vi(previousFace);
                VerboseCodecs.mcBlockPos(buf, blockBreak.position);
                VerboseCodecs.mcBlockPos(buf, lastPos);
                if (flagAndAlert(buf) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                flags.add(new FlagData(face, previousFace, blockBreak.position, lastPos));
            }
        }

        lastFace = blockBreak.face;
        lastPos = blockBreak.position;
        hasBroken = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasBroken = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                var buf = V.write(verbose()).vi(data.face()).vi(data.previousFace());
                VerboseCodecs.mcBlockPos(buf, data.pos());
                VerboseCodecs.mcBlockPos(buf, data.previousPos());
                flagAndAlert(buf);
            }
        }

        flags.clear();
    }

    private record FlagData(int face, int previousFace, Vector3i pos, Vector3i previousPos) {
    }
}