package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

@CheckData(experimental = true)
public class GhostBlockMitigation extends BlockPlaceCheck {

    private boolean enabled;
    public GhostBlockMitigation(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!enabled || player.bukkitPlayer == null) return;

        World world = player.bukkitPlayer.getWorld();
        Vector3i pos = place.getPlacedBlockPos();
        Vector3i posAgainst = place.getPlacedAgainstBlockLocation();

        for (int i = pos.getX() - 2; i <= pos.getX() + 2; i++) {
            for (int j = pos.getY() - 2; j <= pos.getY() + 2; j++) {
                for (int k = pos.getZ() - 2; k <= pos.getZ() + 2; k++) {
                    if (i == pos.getX() && j == pos.getY() && k == pos.getZ()) {
                        continue;
                    }
                    if (i == posAgainst.getX() && j == posAgainst.getY() && k == posAgainst.getZ()) {
                        continue;
                    }
                    Block type = world.getBlockAt(i, j, k);
                    if (type.getType() != Material.AIR) {
                        return;
                    }
                }
            }
        }
        place.resync();

    }

    @Override
    public void reload() {
        super.reload();
        enabled = getConfig().getBooleanElse("exploit.disable-ghostblock-abuses", true);
    }
}
