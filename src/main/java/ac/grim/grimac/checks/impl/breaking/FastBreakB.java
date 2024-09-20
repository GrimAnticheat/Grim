package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "FastBreakB", experimental = true)
public class FastBreakB extends Check implements BlockBreakCheck {
    public FastBreakB(GrimPlayer playerData) {
        super(playerData);
    }

    private Vector3i targetBlock = null;
    private double progress;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return;
        }

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            targetBlock = blockBreak.position;
            progress = BlockBreakSpeed.getBlockDamage(player, targetBlock);
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && progress < 1) {
            if (flagAndAlert(String.format("progress=%.2f", progress)) && shouldModifyPackets()) {
                blockBreak.cancel();
                player.onPacketCancel();
            }
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING && player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
            progress = 0;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (targetBlock == null) {
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (player.compensatedEntities.getSelf().inVehicle() || didRayTraceHit() && isWithinRange()) {
                progress += BlockBreakSpeed.getBlockDamage(player, targetBlock);
            }
        }
    }

    private boolean isWithinRange() {
        double min = Double.MAX_VALUE;
        for (double d : player.getPossibleEyeHeights()) {
            SimpleCollisionBox box = new SimpleCollisionBox(targetBlock);
            Vector eyes = new Vector(player.x, player.y + d, player.z);
            Vector best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        double maxReach = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        double threshold = player.getMovementThreshold();
        maxReach += Math.hypot(threshold, threshold);

        return min <= maxReach * maxReach;
    }

    private boolean didRayTraceHit() {
        SimpleCollisionBox box = new SimpleCollisionBox(targetBlock);

        // Start checking if player is in the block
        double minEyeHeight = Collections.min(player.getPossibleEyeHeights());
        double maxEyeHeight = Collections.max(player.getPossibleEyeHeights());

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.lastX, player.lastY + minEyeHeight, player.lastZ, player.lastX, player.lastY + maxEyeHeight, player.lastZ);
        eyePositions.expand(player.getMovementThreshold());

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(box)) {
            return true;
        }
        // End checking if the player is in the block

        List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
                new Vector3f(player.lastXRot, player.yRot, 0),
                new Vector3f(player.xRot, player.yRot, 0)
        ));

        // 1.9+ players could be a tick behind because we don't get skipped ticks
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs.add(new Vector3f(player.lastXRot, player.lastYRot, 0));
        }

        // 1.7 players do not have any of these issues! They are always on the latest look vector
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            possibleLookDirs = Collections.singletonList(new Vector3f(player.xRot, player.yRot, 0));
        }

        final double distance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        for (double d : player.getPossibleEyeHeights()) {
            for (Vector3f lookDir : possibleLookDirs) {
                Vector3d starting = new Vector3d(player.lastX, player.lastY + d, player.lastZ);
                Ray trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(), lookDir.getX(), lookDir.getY());
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(distance));

                if (intercept.getFirst() != null) return true;
            }
        }

        return false;
    }
}
