package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", experimental = true)
public class MultiBreak extends Check implements BlockBreakCheck {
    private final List<String> flags = new ArrayList<>();
    private boolean hasBroken;
    private BlockFace lastFace;
    private int lastX;
    private int lastY;
    private int lastZ;

    public MultiBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        final int currentX = blockBreak.x;
        final int currentY = blockBreak.y;
        final int currentZ = blockBreak.z;

        if (hasBroken && (blockBreak.face != lastFace ||
                currentX != lastX ||
                currentY != lastY ||
                currentZ != lastZ)) {

            final String currentPosString = MessageUtil.toUnlabeledString(currentX, currentY, currentZ);
            final String lastPosString = MessageUtil.toUnlabeledString(lastX, lastY, lastZ);

            final String verbose = "face=" + blockBreak.face + ", lastFace=" + lastFace
                    + ", pos=" + currentPosString
                    + ", lastPos=" + lastPosString;

            if (!player.canSkipTicks()) {
                if (flagAndAlert(verbose) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                flags.add(verbose);
            }
        }

        lastFace = blockBreak.face;
        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        hasBroken = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.gamemode == GameMode.SPECTATOR || isTickPacket(event.getPacketType())) {
            hasBroken = false;
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
