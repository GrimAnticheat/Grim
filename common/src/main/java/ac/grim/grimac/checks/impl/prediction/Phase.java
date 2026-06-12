package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Phase", stableKey = "grim.prediction.phase", setback = 1, decay = 0.005)
public class Phase extends Check implements PostPredictionCheck {
    private SimpleCollisionBox oldBB;

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
                        WrappedBlockState state = player.compensatedWorld.getBlock((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
                        if (BlockTags.ANVIL.contains(state.getType()) || state.getType() == StateTypes.CHEST || state.getType() == StateTypes.TRAPPED_CHEST) {
                            continue; // 1.8 glitchy block, ignore
                        }
                    }

                    // An open shulker box lid can push players into blocks they weren't previously
                    // intersecting (e.g. the lid pushing a player into a ceiling, or the rebound
                    // clipping the player back into the shulker's own base block).
                    if (isShulkerPushedIntoBlock(newBB, box)) {
                        continue;
                    }
                    flagAndAlertWithSetback();
                    return;
                }
            }
        }

        oldBB = player.boundingBox;
        reward();
    }

    /**
     * Returns true if an open shulker box lid could legitimately have pushed the player
     * into {@code offendingBox}. Two conditions must both hold:
     * <ol>
     *   <li>The player's new bounding box is within the shulker's push zone (the 1×1×1 base
     *       expanded one block in the facing direction), confirming the player is in the lid's
     *       direct influence area.</li>
     *   <li>The offending block is within the shulker's "push column": the shulker's 1×1
     *       cross-section in the axes perpendicular to the push direction, extended to infinity
     *       in the push direction. This allows any block directly in the lid's path (ceiling at
     *       any height above an UP-facing shulker, floor at any depth below a DOWN-facing one)
     *       while rejecting laterally adjacent blocks that the lid can never reach.</li>
     * </ol>
     */
    private boolean isShulkerPushedIntoBlock(SimpleCollisionBox newBB, SimpleCollisionBox offendingBox) {
        for (ShulkerData data : player.compensatedWorld.openShulkerBoxes) {
            if (data.blockPos == null) continue; // entity-backed shulkers handled elsewhere

            WrappedBlockState state = player.compensatedWorld.getBlock(
                    data.blockPos.getX(), data.blockPos.getY(), data.blockPos.getZ());

            BlockFace facing = state.getFacing();
            if (facing == null) facing = BlockFace.UP; // default for shulker boxes

            int bx = data.blockPos.getX(), by = data.blockPos.getY(), bz = data.blockPos.getZ();
            int mx = facing.getModX(), my = facing.getModY(), mz = facing.getModZ();

            // Condition 1: player must be within the push zone (shulker base + 1 block in facing dir).
            SimpleCollisionBox pushZone = new SimpleCollisionBox(
                    bx + Math.min(0, mx), by + Math.min(0, my), bz + Math.min(0, mz),
                    bx + 1 + Math.max(0, mx), by + 1 + Math.max(0, my), bz + 1 + Math.max(0, mz));

            if (!newBB.isIntersected(pushZone)) continue;

            // Condition 2: offending block must be in the push column - the shulker's 1×1 footprint
            // in the perpendicular axes, unbounded in the facing direction. This prevents lateral
            // bypass (e.g. noclipping into an adjacent wall while standing next to the shulker)
            // while correctly exempting ceiling blocks pushed into regardless of their height above.
            SimpleCollisionBox pushColumn = new SimpleCollisionBox(
                    mx < 0 ? -1e8 : bx,
                    my < 0 ? -1e8 : by,
                    mz < 0 ? -1e8 : bz,
                    mx > 0 ? 1e8 : bx + 1,
                    my > 0 ? 1e8 : by + 1,
                    mz > 0 ? 1e8 : bz + 1);

            if (offendingBox.isIntersected(pushColumn)) return true;
        }
        return false;
    }

}
