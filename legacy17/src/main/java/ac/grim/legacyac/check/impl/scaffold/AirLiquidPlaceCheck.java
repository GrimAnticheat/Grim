package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * AirLiquidPlace — Placing a block against air or liquid.
 * Ported from Grim's AirLiquidPlace check.
 *
 * In vanilla, you can only place blocks against a solid surface.
 * Scaffold hacks often place blocks against air or water, which is impossible.
 */
public final class AirLiquidPlaceCheck extends Check {
    public AirLiquidPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AirLiquidPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }

        Block placedAgainst = event.getBlockAgainst();
        if (placedAgainst == null) {
            return;
        }

        Material againstType = placedAgainst.getType();

        if (againstType == Material.AIR) {
            flag(event.getPlayer(), data, 1.0, "placedAgainstAir");
            return;
        }

        if (isLiquid(againstType)) {
            flag(event.getPlayer(), data, 1.0, "placedAgainstLiquid=" + againstType.name());
        }
    }

    private boolean isLiquid(Material mat) {
        return mat == Material.WATER || mat == Material.STATIONARY_WATER
                || mat == Material.LAVA || mat == Material.STATIONARY_LAVA;
    }
}
