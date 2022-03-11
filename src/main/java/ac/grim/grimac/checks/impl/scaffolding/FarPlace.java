package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;

public class FarPlace extends BlockPlaceCheck {
    double pointThree = Math.hypot(0.03, Math.hypot(0.03, 0.03));
    double pointZeroZeroZeroTwo = Math.hypot(0.0002, Math.hypot(0.0002, 0.0002));

    public FarPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3i blockPos = place.getPlacedAgainstBlockLocation();

        if (place.getMaterial() == StateTypes.SCAFFOLDING) return;

        double min = Double.MAX_VALUE;
        for (double d : player.getPossibleEyeHeights()) {
            SimpleCollisionBox box = new SimpleCollisionBox(blockPos);
            Vector eyes = new Vector(player.lastX, player.lastY + d, player.lastZ);
            Vector best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        double maxReach = player.gamemode == GameMode.CREATIVE ? 6.0 : 4.5D;
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)) {
            maxReach += pointZeroZeroZeroTwo;
        } else {
            maxReach += pointThree;
        }

        if (min > maxReach * maxReach) { // fail
            place.resync();
        }
    }
}