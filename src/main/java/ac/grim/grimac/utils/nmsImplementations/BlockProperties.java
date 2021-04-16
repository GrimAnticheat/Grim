package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockFenceGate;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.TagsBlock;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Field;

public class BlockProperties {
    public static float getBlockFriction(GrimPlayer player) {
        return ChunkCache.getBlockDataAt(Math.floor(player.lastX), player.lastY - 0.5000001, Math.floor(player.lastZ)).getBlock().getFrictionFactor();
    }

    // TODO: Compile all these values into an array on startup to improve performance
    public static boolean getCanCollideWith(Object object) {
        Class clazz = object.getClass();

        while (clazz != null) {
            try {
                Field canCollide = clazz.getDeclaredField("at");
                canCollide.setAccessible(true);
                boolean can = canCollide.getBoolean(object);

                return can;
            } catch (NoSuchFieldException | IllegalAccessException noSuchFieldException) {
                clazz = clazz.getSuperclass();
            }
        }

        // We should always be able to get a field
        new Exception().printStackTrace();
        return false;
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        // Use base value because otherwise it isn't async safe.
        // Well, more async safe, still isn't 100% safe.
        if (grimPlayer.lastOnGround) {
            return (float) (grimPlayer.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * (0.21600002f / (f * f * f)) * (grimPlayer.isSprinting ? 1.3 : 1));
        }

        if (grimPlayer.entityPlayer.abilities.isFlying) {
            return grimPlayer.bukkitPlayer.getFlySpeed() * 10 * (grimPlayer.isSprinting ? 0.1f : 0.05f);

        } else {
            if (grimPlayer.isSprinting) {
                return 0.026f;
            } else {
                return 0.02f;
            }
        }
    }

    // Entity line 617
    public static Block getOnBlock(Location getBlockLocation) {
        IBlockData block1 = ChunkCache.getBlockDataAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 0.2F), getBlockLocation.getBlockZ());

        if (block1.isAir()) {
            Block block2 = ChunkCache.getBlockDataAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 1.2F), getBlockLocation.getBlockZ()).getBlock();

            if (block2.a(TagsBlock.FENCES) || block2.a(TagsBlock.WALLS) || block2 instanceof BlockFenceGate) {
                return block2;
            }
        }

        return block1.getBlock();
    }

    // Entity line 637
    public static float getBlockSpeedFactor(GrimPlayer player) {
        if (player.bukkitPlayer.isGliding() || player.isFlying) return 1.0f;

        net.minecraft.server.v1_16_R3.Block block = ChunkCache.getBlockDataAt(player.x, player.y, player.z).getBlock();

        if (block.a(TagsBlock.SOUL_SPEED_BLOCKS)) {
            if (player.bukkitPlayer.getInventory().getBoots() != null && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
        }

        float f = block.getSpeedFactor();

        if (block == net.minecraft.server.v1_16_R3.Blocks.WATER || block == net.minecraft.server.v1_16_R3.Blocks.BUBBLE_COLUMN) {
            return f;
        }

        return f == 1.0 ? ChunkCache.getBlockDataAt(player.x, player.y - 0.5000001, player.z).getBlock().getSpeedFactor() : f;
    }
}
