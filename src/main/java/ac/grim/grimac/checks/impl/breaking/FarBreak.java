package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import org.bukkit.util.Vector;

@CheckData(name = "FarBreak", checkType = CheckType.WORLD, description = "Breaking blocks too far away", experimental = true)
public class FarBreak extends Check implements BlockBreakCheck {
    public FarBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.gamemode == GameMode.SPECTATOR || player.inVehicle() || blockBreak.action == DiggingAction.CANCELLED_DIGGING) return; // falses

        double min = Double.MAX_VALUE;
        for (double d : player.getPossibleEyeHeights()) {
            SimpleCollisionBox box = new SimpleCollisionBox(blockBreak.position);
            Vector eyes = new Vector(player.x, player.y + d, player.z);
            Vector best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        if (player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks()) {
            double threshold = player.getMovementThreshold();
            maxReach += Math.hypot(threshold, threshold);
        }

        if (min > maxReach * maxReach && flagAndAlert(String.format("distance=%.2f", Math.sqrt(min))) && shouldModifyPackets()) {
            blockBreak.cancel();
        }
    }
}
