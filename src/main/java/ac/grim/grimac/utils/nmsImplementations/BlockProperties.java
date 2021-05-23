package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Location;
import org.bukkit.Material;
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
            alsoWater = Materials.matchLegacy("STATIONARY_WATER");
        }
    }

    public static float getBlockFrictionUnderPlayer(GrimPlayer player) {
        if (XMaterial.getVersion() > 8 && (player.bukkitPlayer.isGliding() || player.specialFlying)) return 1.0f;

        double searchBelowAmount = 0.5000001;

        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_15))
            searchBelowAmount = 1;

        Material material = player.compensatedWorld.getBukkitMaterialAt(player.lastX, player.lastY - searchBelowAmount, player.lastZ);

        return getMaterialFriction(player, material);
    }

    public static float getMaterialFriction(GrimPlayer player, Material material) {
        float friction = 0.6f;

        if (material == ice) friction = 0.98f;
        if (material == slime && player.clientVersion >= 8) friction = 0.8f;
        if (material == packedIce) friction = 0.98f;
        if (material == frostedIce) friction = 0.98f;
        if (material == blueIce) {
            friction = 0.98f;
            if (player.clientVersion >= 13) friction = 0.989f;
        }

        return friction;
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer player) {
        //Player bukkitPlayer = player.bukkitPlayer;

        // Use base value because otherwise it isn't async safe.
        // Well, more async safe, still isn't 100% safe.
        if (player.lastOnGround) {
            return (float) (player.movementSpeed * (0.21600002f / (f * f * f)));
        }

        if (player.specialFlying) {
            return player.flySpeed * 20 * (player.isSprinting ? 0.1f : 0.05f);

        } else {
            if (player.isSprinting) {
                return 0.026f;
            } else {
                return 0.02f;
            }
        }
    }

    // Entity line 617
    public static Material getOnBlock(GrimPlayer player, Location getBlockLocation) {
        Material block1 = player.compensatedWorld.getBukkitMaterialAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 0.2F), getBlockLocation.getBlockZ());

        if (Materials.checkFlag(block1, Materials.AIR)) {
            Material block2 = player.compensatedWorld.getBukkitMaterialAt(getBlockLocation.getBlockX(), (int) Math.floor(getBlockLocation.getY() - 1.2F), getBlockLocation.getBlockZ());

            if (Materials.checkFlag(block2, Materials.FENCE) || Materials.checkFlag(block2, Materials.WALL) || Materials.checkFlag(block2, Materials.GATE)) {
                return block2;
            }
        }

        return block1;
    }

    // Entity line 637
    public static float getBlockSpeedFactor(GrimPlayer player) {
        if (XMaterial.getVersion() > 8 && (player.bukkitPlayer.isGliding() || player.specialFlying)) return 1.0f;

        Material block = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y, player.z);

        if (block == soulSand) {
            // Soul speed is a 1.16+ enchantment
            if (player.bukkitPlayer.getInventory().getBoots() != null && XMaterial.getVersion() > 15 && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
            return 0.4f;
        }

        float f = 1.0f;

        if (block == honeyBlock) f = 0.4F;

        if (block == water || block == alsoWater) {
            return f;
        }

        if (f == 1.0) {
            Material block2 = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y - 0.5000001, player.z);
            if (block2 == honeyBlock) return 0.4F;
            if (block2 == soulSand) return 0.4F;
            return 1.0f;
        }

        return f;
    }
}
