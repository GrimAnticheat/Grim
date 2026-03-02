package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
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
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, event.getTo(), data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

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
        if (!material.isSolid()) {
            return false;
        }
        String name = material.name();
        if (name.contains("SIGN") || name.contains("STEP") || name.contains("SLAB")
            || name.contains("STAIR") || name.contains("FENCE") || name.contains("GATE")
            || name.contains("DOOR") || name.contains("PISTON") || name.contains("CHEST")
            || name.contains("ANVIL") || name.contains("SKULL") || name.contains("HEAD")
            || name.contains("FLOWER_POT") || name.contains("BREWING")
            || name.contains("ENCHANT") || name.contains("CAULDRON")
            || name.contains("BED") || name.contains("CAKE")
            || name.contains("TRAP") || name.contains("COBBLE_WALL")
            || name.contains("CARPET") || name.contains("SNOW")
            || name.contains("DAYLIGHT") || name.contains("HOPPER")
            || name.contains("DRAGON_EGG") || name.contains("ENDER_PORTAL_FRAME")) {
            return false;
        }
        return material != Material.WATER && material != Material.LAVA && material != Material.WEB;
    }
}
