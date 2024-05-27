package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class GhostBlockMitigation extends BlockPlaceCheck {

    private boolean allow;
    private int distance;

    public GhostBlockMitigation(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (allow || player.bukkitPlayer == null) return;

        final World world = player.bukkitPlayer.getWorld();
        final Vector3i pos = place.getPlacedBlockPos();
        final Vector3i posAgainst = place.getPlacedAgainstBlockLocation();

        final int x = pos.getX(),
                y = pos.getY(),
                z = pos.getZ();

        final int xAgainst = posAgainst.getX(),
                yAgainst = posAgainst.getY(),
                zAgainst = posAgainst.getZ();

        boolean loaded = false;

        try {
            for (int i = x - distance; i <= x + distance; i++) {
                for (int j = y - distance; j <= y + distance; j++) {
                    for (int k = z - distance; k <= z + distance; k++) {
                        if (i == x && j == y && k == z) {
                            continue;
                        }
                        if (i == xAgainst && j == yAgainst && k == zAgainst) {
                            continue;
                        }
                        if (!loaded && world.isChunkLoaded(x >> 4, z >> 4)) {
                            loaded = true;
                            continue;
                        }
                        final Block type = world.getBlockAt(i, j, k);
                        if (type.getType() != Material.AIR) {
                            return;
                        }
                    }
                }
            }

            place.resync();
        } catch (final Exception ignored) {
        }
    }

    @Override
    public void reload() {
        super.reload();
        allow = getConfig().getBooleanElse("exploit.allow-building-on-ghostblocks", true);
        distance = getConfig().getIntElse("exploit.distance-to-check-for-ghostblocks", 2);

        if (distance < 2 || distance > 4) distance = 2;
    }
}
