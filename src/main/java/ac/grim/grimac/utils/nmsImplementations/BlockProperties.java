package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.enchantments.Enchantment;

public class BlockProperties {
    private final static Material ice = XMaterial.ICE.parseMaterial();
    private final static Material slime = XMaterial.SLIME_BLOCK.parseMaterial();
    private final static Material packedIce = XMaterial.PACKED_ICE.parseMaterial();
    private final static Material frostedIce = XMaterial.FROSTED_ICE.parseMaterial();
    private final static Material blueIce = XMaterial.BLUE_ICE.parseMaterial();

    private final static Material soulSand = XMaterial.SOUL_SAND.parseMaterial();
    private final static Material honeyBlock = XMaterial.HONEY_BLOCK.parseMaterial();

    // WATER and STATIONARY_WATER on 1.12
    // WATER and BUBBLE_COLUMN on 1.13
    private final static Material water;
    private final static Material alsoWater;

    static {
        if (XMaterial.isNewVersion()) {
            water = Material.WATER;
            alsoWater = Material.BUBBLE_COLUMN;
        } else {
            water = Material.WATER;
            alsoWater = Material.LEGACY_STATIONARY_WATER;
        }
    }

    public static float getBlockFriction(GrimPlayer player) {
        if (player.bukkitPlayer.isGliding() || player.specialFlying) return 1.0f;

        Material material = ChunkCache.getBukkitBlockDataAt(player.lastX, player.lastY - 0.5000001, player.lastZ).getMaterial();

        float friction = 0.6f;

        if (material == ice) friction = 0.98f;
        if (material == slime) friction = 0.8f;
        if (material == packedIce) friction = 0.98f;
        if (material == frostedIce) friction = 0.98f;
        if (material == blueIce) {
            friction = 0.98f;
            if (player.clientVersion >= 13) friction = 0.989f;
        }

        return friction;
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        // Use base value because otherwise it isn't async safe.
        // Well, more async safe, still isn't 100% safe.
        if (grimPlayer.lastOnGround) {
            return (float) (grimPlayer.movementSpeed * (0.21600002f / (f * f * f)));
        }

        if (grimPlayer.specialFlying) {
            return grimPlayer.flySpeed * 20 * (grimPlayer.isSprinting ? 0.1f : 0.05f);

        } else {
            if (grimPlayer.isSprinting) {
                return 0.026f;
            } else {
                return 0.02f;
            }
        }
    }

    // Entity line 617
    public static BlockData getOnBlock(Location getBlockLocation) {
        BlockData block1 = ChunkCache.getBukkitBlockDataAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 0.2F), getBlockLocation.getBlockZ());

        if (block1.getMaterial().isAir()) {
            BlockData block2 = ChunkCache.getBukkitBlockDataAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 1.2F), getBlockLocation.getBlockZ());

            if (block2 instanceof Fence || block2 instanceof Wall || block2 instanceof Gate) {
                return block2;
            }
        }

        return block1;
    }

    // Entity line 637
    public static float getBlockSpeedFactor(GrimPlayer player) {
        if (player.bukkitPlayer.isGliding() || player.specialFlying) return 1.0f;

        Material block = ChunkCache.getBukkitBlockDataAt(player.x, player.y, player.z).getMaterial();

        if (block == soulSand) {
            if (player.bukkitPlayer.getInventory().getBoots() != null && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
            return 0.4f;
        }

        float f = 1.0f;

        if (block == honeyBlock) f = 0.4F;

        if (block == water || block == alsoWater) {
            return f;
        }

        if (f == 1.0) {
            Material block2 = ChunkCache.getBukkitBlockDataAt(player.x, player.y - 0.5000001, player.z).getMaterial();
            if (block2 == honeyBlock) return 0.4F;
            if (block2 == soulSand) return 0.4F;
            return 1.0f;
        }

        return f;
    }
}
