package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.util.Vector;

@CheckData(name = "FastBreak")
public class FastBreak extends Check implements BlockBreakCheck {
    public FastBreak(GrimPlayer playerData) {
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
            // check if the player is within range of the block
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

            if (min > maxReach * maxReach) {
                return;
            }

            progress += BlockBreakSpeed.getBlockDamage(player, targetBlock);
        }
    }
}
