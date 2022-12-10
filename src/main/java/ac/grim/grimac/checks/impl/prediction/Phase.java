package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Phase", configName = "Phase", setback = 1, decay = 0.005)
public class Phase extends Check implements PostPredictionCheck {
    SimpleCollisionBox oldBB;

    public Phase(GrimPlayer player) {
        super(player);
        oldBB = player.boundingBox;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!player.getSetbackTeleportUtil().blockOffsets && !predictionComplete.getData().isTeleport() && predictionComplete.isChecked()) { // Not falling through world
            SimpleCollisionBox newBB = player.boundingBox;

            List<SimpleCollisionBox> boxes = new ArrayList<>();
            Collisions.getCollisionBoxes(player, newBB, boxes, false);

            for (SimpleCollisionBox box : boxes) {
                if (newBB.isIntersected(box) && !oldBB.isIntersected(box)) {
                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                        // A bit of a hacky way to get the block state, but this is much faster to use the tuinity method for grabbing collision boxes
                        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
                        if (BlockTags.ANVIL.contains(state.getType()) || state.getType() == StateTypes.CHEST || state.getType() == StateTypes.TRAPPED_CHEST) {
                            continue; // 1.8 glitchy block, ignore
                        }
                    }
                    if (flagWithSetback())
                        alert("");
                    return;
                }
            }
        }

        oldBB = player.boundingBox;
        reward();
    }
}
