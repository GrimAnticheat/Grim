package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", experimental = true)
public class MultiBreak extends Check implements BlockBreakCheck {
    public MultiBreak(GrimPlayer player) {
        super(player);
    }

    private final List<String> flags = new ArrayList<>();

    private boolean hasBroken;
    private BlockFace lastFace;
    private Vector3i lastPos;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        if (hasBroken && (blockBreak.face != lastFace || !blockBreak.position.equals(lastPos))) {
            final String verbose = "face=" + blockBreak.face + ", lastFace=" + lastFace
                    + ", pos=" + MessageUtil.toUnlabledString(blockBreak.position) + ", lastPos=" + MessageUtil.toUnlabledString(lastPos);
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                if (flagAndAlert(verbose) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                flags.add(verbose);
            }
        }

        lastFace = blockBreak.face;
        lastPos = blockBreak.position;
        hasBroken = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            hasBroken = false;
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
