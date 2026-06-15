package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiPlace", stableKey = "grim.scaffolding.multi_place", description = "Placed multiple blocks in a tick", experimental = true)
public class MultiPlace extends BlockPlaceCheck {
    private static final Verbose V =
            Verbose.of("face={face}, lastFace={face}, cursor={cursor}, lastCursor={cursor}, pos={mcpos}, lastPos={mcpos}");

    private final List<FlagData> flags = new ArrayList<>();
    private boolean hasPlaced;
    private BlockFace lastFace;
    private Vector3f lastCursor;
    private Vector3i lastPos;

    public MultiPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final BlockFace face = place.getFace();
        final Vector3f cursor = place.cursor;
        final Vector3i pos = place.position;

        if (hasPlaced && (face != lastFace || !cursor.equals(lastCursor) || !pos.equals(lastPos))) {
            final int faceId = VerboseCodecs.enumId(face);
            final int lastFaceId = VerboseCodecs.enumId(lastFace);
            if (!player.canSkipTicks()) {
                var buf = V.write(verbose()).uint(faceId).uint(lastFaceId)
                        .cursor(cursor.x, cursor.y, cursor.z)
                        .cursor(lastCursor.x, lastCursor.y, lastCursor.z)
                        .mcPos(pos.x, pos.y, pos.z)
                        .mcPos(lastPos.x, lastPos.y, lastPos.z);
                if (flag(buf)
                        && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.add(new FlagData(faceId, lastFaceId, cursor, lastCursor, pos, lastPos));
            }
        }

        lastFace = face;
        lastCursor = cursor;
        lastPos = pos;
        hasPlaced = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasPlaced = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flag(V.write(verbose()).uint(data.face()).uint(data.lastFace())
                        .cursor(data.cursor().x, data.cursor().y, data.cursor().z)
                        .cursor(data.lastCursor().x, data.lastCursor().y, data.lastCursor().z)
                        .mcPos(data.pos().x, data.pos().y, data.pos().z)
                        .mcPos(data.lastPos().x, data.lastPos().y, data.lastPos().z));
            }
        }

        flags.clear();
    }

    private record FlagData(
            int face,
            int lastFace,
            Vector3f cursor,
            Vector3f lastCursor,
            Vector3i pos,
            Vector3i lastPos) {
    }
}
