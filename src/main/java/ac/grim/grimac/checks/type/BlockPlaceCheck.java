package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceCheck extends Check<BlockPlace> {
    private static final List<StateType> weirdBoxes = new ArrayList<>();
    private static final List<StateType> buggyBoxes = new ArrayList<>();

    public BlockPlaceCheck(GrimPlayer player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
    }

    static {
        // Fences and walls aren't worth checking.
        weirdBoxes.addAll(new ArrayList<>(BlockTags.FENCES.getStates()));
        weirdBoxes.addAll(new ArrayList<>(BlockTags.WALLS.getStates()));

        buggyBoxes.addAll(new ArrayList<>(BlockTags.DOORS.getStates()));
        buggyBoxes.addAll(new ArrayList<>(BlockTags.STAIRS.getStates()));
        buggyBoxes.add(StateTypes.CHEST);
        buggyBoxes.add(StateTypes.TRAPPED_CHEST);
        buggyBoxes.add(StateTypes.CHORUS_PLANT);
    }

    protected SimpleCollisionBox getCombinedBox(final BlockPlace place) {
        // Alright, instead of skidding AACAdditionsPro, let's just use bounding boxes
        Vector3i clicked = place.getPlacedAgainstBlockLocation();
        CollisionBox placedOn = HitboxData.getBlockHitbox(player, place.getMaterial(), player.getClientVersion(), player.compensatedWorld.getWrappedBlockStateAt(clicked), clicked.getX(), clicked.getY(), clicked.getZ());

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        placedOn.downCast(boxes);

        SimpleCollisionBox combined = new SimpleCollisionBox(clicked.getX(), clicked.getY(), clicked.getZ());
        for (SimpleCollisionBox box : boxes) {
            double minX = Math.max(box.minX, combined.minX);
            double minY = Math.max(box.minY, combined.minY);
            double minZ = Math.max(box.minZ, combined.minZ);
            double maxX = Math.min(box.maxX, combined.maxX);
            double maxY = Math.min(box.maxY, combined.maxY);
            double maxZ = Math.min(box.maxZ, combined.maxZ);
            combined = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        if (weirdBoxes.contains(place.getPlacedAgainstMaterial())) {
            combined = new SimpleCollisionBox(clicked.getX(), clicked.getY(), clicked.getZ(), clicked.getX() + 1, clicked.getY() + 1.5, clicked.getZ() + 1);
        }

        if (buggyBoxes.contains(place.getPlacedAgainstMaterial())) {
            combined = new SimpleCollisionBox(clicked.getX(), clicked.getY(), clicked.getZ());
        }

        return combined;
    }
}
