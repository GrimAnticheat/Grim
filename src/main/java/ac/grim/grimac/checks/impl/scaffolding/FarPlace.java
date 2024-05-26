package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;

@CheckData(name = "FarPlace")
public class FarPlace extends BlockPlaceCheck {
    public FarPlace(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final Vector3i blockPos = place.getPlacedAgainstBlockLocation();

        if (place.getMaterial() == StateTypes.SCAFFOLDING) return;

        double min = Double.MAX_VALUE;
        for (final double d : player.getPossibleEyeHeights()) {
            final SimpleCollisionBox box = new SimpleCollisionBox(blockPos);
            final Vector eyes = new Vector(player.x, player.y + d, player.z);
            final Vector best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        final boolean creativeReach = player.gamemode == GameMode.CREATIVE &&
                !player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5);
        final double threshold = player.getMovementThreshold();
        final double maxReach = Math.hypot(threshold, threshold)
                + (creativeReach ? 6.0 : player.compensatedEntities.getSelf().getBlockInteractRange());

        if (min <= maxReach * maxReach) return;

        // fail
        if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }
}
