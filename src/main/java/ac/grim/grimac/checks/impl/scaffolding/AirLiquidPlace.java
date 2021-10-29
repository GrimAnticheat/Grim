package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.nmsutil.Materials;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Material;

public class AirLiquidPlace extends BlockPlaceCheck {
    public AirLiquidPlace(GrimPlayer player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
        Vector3i blockPos = place.getPlacedAgainstBlockLocation();
        Material placeAgainst = player.compensatedWorld.getBukkitMaterialAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if ((Materials.checkFlag(placeAgainst, Materials.AIR) || Materials.isNoPlaceLiquid(placeAgainst))) { // fail
            place.resync();
        }
    }
}
