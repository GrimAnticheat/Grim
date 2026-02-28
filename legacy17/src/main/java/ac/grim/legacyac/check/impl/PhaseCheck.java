package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PhaseCheck extends Check {
    public PhaseCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Phase");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }
        if (player.getVehicle() != null || player.isInsideVehicle()) {
            return;
        }

        Block feet = to.getBlock();
        Block head = to.clone().add(0.0D, 1.0D, 0.0D).getBlock();
        if (isSolid(feet.getType()) || isSolid(head.getType())) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.Phase.buffer", 1.5D)) {
                flag(player, data, 1.0D, "inside=" + feet.getType().name() + "/" + head.getType().name());
            }
        }
    }

    private boolean isSolid(Material material) {
        return material.isSolid() && material != Material.WATER && material != Material.LAVA && material != Material.WEB;
    }
}
