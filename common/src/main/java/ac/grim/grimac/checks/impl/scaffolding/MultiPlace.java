package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
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

@CheckData(name = "MultiPlace", stableKey = "grim.scaffolding.multi_place", verboseVersion = 2, description = "Placed multiple blocks in a tick", experimental = true)
public class MultiPlace extends BlockPlaceCheck {
    public static final VerboseSchema V = VerboseSchema.of(2,
            "face:enum", "lastFace:enum",
            "cursorX:f32", "cursorY:f32", "cursorZ:f32",
            "lastCursorX:f32", "lastCursorY:f32", "lastCursorZ:f32",
            "posXZ:vl", "posY:zz", "lastPosXZ:vl", "lastPosY:zz");

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
            final int faceId = VerboseCodecs.enumOrdinal(face);
            final int lastFaceId = VerboseCodecs.enumOrdinal(lastFace);
            if (!player.canSkipTicks()) {
                var buf = V.write(verbose()).vi(faceId).vi(lastFaceId);
                VerboseCodecs.cursor3f(buf, cursor);
                VerboseCodecs.cursor3f(buf, lastCursor);
                VerboseCodecs.mcBlockPos(buf, pos);
                VerboseCodecs.mcBlockPos(buf, lastPos);
                if (flagAndAlert(buf)
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
                var buf = V.write(verbose()).vi(data.face()).vi(data.lastFace());
                VerboseCodecs.cursor3f(buf, data.cursor());
                VerboseCodecs.cursor3f(buf, data.lastCursor());
                VerboseCodecs.mcBlockPos(buf, data.pos());
                VerboseCodecs.mcBlockPos(buf, data.lastPos());
                flagAndAlert(buf);
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
