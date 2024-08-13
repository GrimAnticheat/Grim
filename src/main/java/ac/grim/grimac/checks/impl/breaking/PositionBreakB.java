package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "PositionBreakB")
public class PositionBreakB extends BlockBreakCheck {
    public PositionBreakB(GrimPlayer player) {
        super(player);
    }

    private BlockFace lastFace;
    private final BlockFace releaseFace = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? BlockFace.DOWN : BlockFace.SOUTH;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            flagAndAlert();
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            lastFace = blockBreak.face == releaseFace ? null : blockBreak.face;
        }
    }
}
