package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import org.bukkit.util.Vector;

@CheckData(name = "FarBreak")
public class FarBreak extends Check implements BlockBreakCheck {
    public FarBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.compensatedEntities.getSelf().inVehicle()) return; // falses

        double x = player.x;
        double y = player.y;
        double z = player.z;

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            if (blockBreak.isWeirdCancel) {
                return;
            }

            x = player.lastX;
            y = player.lastY;
            z = player.lastZ;
        }

        double min = Double.MAX_VALUE;
        for (double d : player.getPossibleEyeHeights()) {
            SimpleCollisionBox box = new SimpleCollisionBox(blockBreak.position);
            Vector eyes = new Vector(x, y + d, z);
            Vector best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        double maxReach = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        double threshold = player.getMovementThreshold();
        maxReach += Math.hypot(threshold, threshold);

        if (min > maxReach * maxReach) {
            if (flagAndAlert("distance=" + Math.sqrt(min)) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
