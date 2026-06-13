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

@CheckData(name = "InvalidBreak", stableKey = "grim.breaking.invalid_break", verboseVersion = 2, description = "Sent impossible block face id")
public class InvalidBreak extends Check implements BlockBreakCheck {
    public static final VerboseSchema V = VerboseSchema.of(2, "faceId:zz", "action:enum");

    public InvalidBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.faceId == 255 && blockBreak.action == DiggingAction.CANCELLED_DIGGING && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
            return;
        }

        if (blockBreak.faceId < 0 || blockBreak.faceId > 5) {
            // ban
            int action = VerboseCodecs.enumOrdinal(blockBreak.action);
            if (flagAndAlert(V.write(verbose()).zz(blockBreak.faceId).vi(action)) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
