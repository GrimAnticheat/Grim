package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PostBlockPlace;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class BlockPlaceCheck extends Check implements RotationCheck {
    private static final List<StateType> weirdBoxes = new ArrayList<>();
    private static final List<StateType> buggyBoxes = new ArrayList<>();

    protected int cancelVL;

    public BlockPlaceCheck(GrimPlayer player) {
        super(player);
    }

    // Method called immediately after a block is placed, before forwarding block place to server
    public void onBlockPlace(final BlockPlace place) {
    }

    // Method called the flying packet after the block place
    public void onPostFlyingBlockPlace(PostBlockPlace place) {
    }

    @Override
    public void reload() {
        super.reload();
        this.cancelVL = getConfig().getIntElse(getConfigName() + ".cancelVL", 5);
    }

    protected boolean shouldCancel() {
        return cancelVL >= 0 && violations >= cancelVL;
    }

    static {
        // Fences and walls aren't worth checking.
        weirdBoxes.addAll(new ArrayList<>(BlockTags.FENCES.getStates()));
        weirdBoxes.addAll(new ArrayList<>(BlockTags.WALLS.getStates()));
        weirdBoxes.add(StateTypes.LECTERN);

        buggyBoxes.addAll(new ArrayList<>(BlockTags.DOORS.getStates()));
        buggyBoxes.addAll(new ArrayList<>(BlockTags.STAIRS.getStates()));
        buggyBoxes.add(StateTypes.CHEST);
        buggyBoxes.add(StateTypes.TRAPPED_CHEST);
        buggyBoxes.add(StateTypes.CHORUS_PLANT);

        // The client changes these block states around when placing blocks, temporary desync
        buggyBoxes.add(StateTypes.KELP);
        buggyBoxes.add(StateTypes.KELP_PLANT);
        buggyBoxes.add(StateTypes.TWISTING_VINES);
        buggyBoxes.add(StateTypes.TWISTING_VINES_PLANT);
        buggyBoxes.add(StateTypes.WEEPING_VINES);
        buggyBoxes.add(StateTypes.WEEPING_VINES_PLANT);
        buggyBoxes.add(StateTypes.REDSTONE_WIRE);
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
            // Invert the box to give lenience
            combined = new SimpleCollisionBox(clicked.getX() + 1, clicked.getY() + 1, clicked.getZ() + 1, clicked.getX(), clicked.getY() + 1.5, clicked.getZ());
        }

        if (buggyBoxes.contains(place.getPlacedAgainstMaterial())) {
            // Invert the bounding box to give a block of lenience
            combined = new SimpleCollisionBox(clicked.getX() + 1, clicked.getY() + 1, clicked.getZ() + 1, clicked.getX(), clicked.getY(), clicked.getZ());
        }

        return combined;
    }
}
