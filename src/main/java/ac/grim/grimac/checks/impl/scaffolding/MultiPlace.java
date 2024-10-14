package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiPlace", experimental = true)
public class MultiPlace extends BlockPlaceCheck {
    public MultiPlace(GrimPlayer player) {
        super(player);
    }

    private final List<String> flags = new ArrayList<>();

    private boolean hasPlaced;
    private BlockFace lastFace;
    private Vector3f lastCursor;
    private Vector3i lastPos;

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final BlockFace face = place.getDirection();
        final Vector3f cursor = place.getCursor();
        final Vector3i pos = place.getPlacedAgainstBlockLocation();

        if (hasPlaced && (face != lastFace || !cursor.equals(lastCursor) || !pos.equals(lastPos))) {
            final String verbose = "face=" + face + ", lastFace=" + lastFace
                    + ", cursor=" + MessageUtil.toUnlabledString(cursor) + ", lastCursor=" + MessageUtil.toUnlabledString(lastCursor)
                    + ", pos=" + MessageUtil.toUnlabledString(pos) + ", lastPos=" + MessageUtil.toUnlabledString(lastPos);
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                if (flagAndAlert(verbose) && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.add(verbose);
            }
        }

        lastFace = face;
        lastCursor = cursor;
        lastPos = pos;
        hasPlaced = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            hasPlaced = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) && !player.skippedTickInActualMovement && predictionComplete.isChecked()) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
