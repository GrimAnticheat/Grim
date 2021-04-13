package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;

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

        if (grimPlayer.lastOnGround) {
            return (float) (grimPlayer.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (0.21600002f / (f * f * f)));
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
    public static float getBlockSpeedFactor(GrimPlayer player) {
        net.minecraft.server.v1_16_R3.Block block = ChunkCache.getBlockDataAt(player.lastX, player.lastY, player.lastZ).getBlock();

        float f = block.getSpeedFactor();

        if (block == net.minecraft.server.v1_16_R3.Blocks.WATER || block == net.minecraft.server.v1_16_R3.Blocks.BUBBLE_COLUMN) {
            return f;
        }

        return f == 1.0 ? ChunkCache.getBlockDataAt(player.lastX, player.boundingBox.minY - 0.5000001, player.lastZ).getBlock().getSpeedFactor() : f;
    }
}
