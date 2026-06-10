package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "PositionBreakB", stableKey = "grim.breaking.position_break_b", verboseVersion = 2, description = "Cancelled block breaking with an invalid block face")
public class PositionBreakB extends Check implements BlockBreakCheck {
    public static final VerboseSchema V = VerboseSchema.of(2, "lastFace:enum", "action:enum");

    private final boolean allowLegacyFace = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10);
    private BlockFace lastFace;

    public PositionBreakB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            int lastFaceId = VerboseCodecs.enumOrdinal(lastFace);
            int action = VerboseCodecs.enumOrdinal(blockBreak.action);
            flagAndAlert(V.write(verbose()).vi(lastFaceId).vi(action));
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            // as of https://github.com/ViaVersion/ViaRewind/commit/e7b0606e187afbccf98ef7c88d3f3af27fe11da3,
            // ViaRewind maps face 255 for 1.7 clients to 0. Let's allow both, just to be safe
            lastFace = blockBreak.faceId == 0 || allowLegacyFace && blockBreak.faceId == 255 ? null : blockBreak.face;
        }
    }
}