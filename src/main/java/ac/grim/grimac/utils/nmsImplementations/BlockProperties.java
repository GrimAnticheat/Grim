package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;

public class BlockProperties {
    // TODO: this code is shit
    // Seems to work.
    public static float getBlockFriction(Player bukkitPlayer) {
        return ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt(
                bukkitPlayer.getLocation().getBlockX(),
                (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getFrictionFactor();
    }

    // Verified.  This is correct.
    public static float getFrictionInfluencedSpeed(float f, GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;

        if (grimPlayer.lastOnGround) {
            return (float) (bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (0.21600002f / (f * f * f)));
        }

        // TODO: This is wrong
        if (grimPlayer.entityPlayer.abilities.isFlying) {
            return bukkitPlayer.getFlySpeed() * 10 * (grimPlayer.bukkitPlayer.isSprinting() ? 0.1f : 0.05f);

        } else {
            if (bukkitPlayer.isSprinting()) {
                return 0.026f;
            } else {
                return 0.02f;
            }
        }
    }

    // Entity line 617
    // Heavily simplified (wtf was that original code mojang)
    public static Block getOnBlock(Location getBlockLocation) {
        Block block1 = getBlockLocation.getWorld().getBlockAt(getBlockLocation.getBlockX(), (int) (getBlockLocation.getY() - 0.2F), getBlockLocation.getBlockZ());
        Block block2 = getBlockLocation.getWorld().getBlockAt(getBlockLocation.getBlockX(), (int) (getBlockLocation.getY() - 1.2F), getBlockLocation.getBlockZ());

        if (block2.getType().isAir()) {
            if (block2 instanceof Fence || block2 instanceof Wall || block2 instanceof Gate) {
                return block2;
            }
        }

        return block1;
    }

    // Entity line 637
    // Seems fine to me.  Haven't found issues here
    public static float getBlockSpeedFactor(Player bukkitPlayer) {
        net.minecraft.server.v1_16_R3.Block block = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(),
                        bukkitPlayer.getLocation().getBlockY(),
                        bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock();

        float f = block.getSpeedFactor();

        if (block == net.minecraft.server.v1_16_R3.Blocks.WATER || block == net.minecraft.server.v1_16_R3.Blocks.BUBBLE_COLUMN) {
            return f;
        }

        return (double) f == 1.0 ? ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                        bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getSpeedFactor() : f;
    }
}
