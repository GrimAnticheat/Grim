package ac.grim.grimac.checks.impl.ghosthand;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.events.packets.CheckManagerListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "GhostHand", configName = "GhostHand", experimental = true)
public class GhostHand extends BlockPlaceCheck {
    double flagBuffer = 0; // If the player flags once, force them to play legit, or we will cancel the tick before.
    boolean ignorePost = false;

    public GhostHand(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (!isInteracting(place)) return;

        if (flagBuffer > 0 && !didRayTraceHit(place)) {
            ignorePost = true;
            // If the player hit and has flagged this check recently
            if (flagAndAlert("pre-flying") && shouldModifyPackets() && shouldCancel()) {
                place.resync();  // Deny the block interact.
            }
        }
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (!isInteracting(place)) return;

        // Don't flag twice
        if (ignorePost) {
            ignorePost = false;
            return;
        }

        // Ray trace to try and hit the target block.
        boolean hit = didRayTraceHit(place);
        // This can false with rapidly moving yaw in 1.8+ clients
        if (!hit) {
            flagBuffer = 1;
            flagAndAlert("post-flying");
        } else {
            flagBuffer = Math.max(0, flagBuffer - 0.1);
        }
    }

    private boolean isInteracting(BlockPlace place) {
        StateType placedAgainst = place.getPlacedAgainstMaterial();
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_8) && (placedAgainst == StateTypes.IRON_TRAPDOOR || placedAgainst == StateTypes.IRON_DOOR)
                || Materials.isClientSideInteractable(placedAgainst);
    }

    private boolean didRayTraceHit(BlockPlace place) {
        HitData hitData = CheckManagerListener.getNearestHitResult(player, StateTypes.AIR, false);
        return hitData != null && hitData.getPosition().equals(place.getPlacedAgainstBlockLocation());
    }
}
