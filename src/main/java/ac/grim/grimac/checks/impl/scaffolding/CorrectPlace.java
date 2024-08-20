package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3i;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.Material;

@CheckData(name = "CorrectPlace", experimental = true)
public class CorrectPlace extends BlockPlaceCheck {

    private Vector3i previousBlockPlaced = null;

    public CorrectPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace event) {
        if (player.y < event.getPlacedBlockPos().getY()) return;
        if (player.yRot < 82.6 && player.yRot > 76.7) return;

        Vector3i pos = event.getPlacedBlockPos();

        Location loc = new Location(player.bukkitPlayer.getWorld(), pos.getX(), pos.getY(), pos.getZ());

        if (!loc.subtract(0, 0.6, 0).getBlock().getType().equals(Material.AIR)) return;

        if (previousBlockPlaced != null && previousBlockPlaced.getY() - event.getPlacedBlockPos().getY() == 0) {
            if (flagAndAlert() && shouldCancel() && shouldModifyPackets()) {
                event.resync();
            }
        }
        previousBlockPlaced = event.getPlacedBlockPos();
    }

}
