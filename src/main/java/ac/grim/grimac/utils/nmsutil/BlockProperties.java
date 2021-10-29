package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public class BlockProperties {
    private static final Material ICE = XMaterial.ICE.parseMaterial();
    private static final Material SLIME = XMaterial.SLIME_BLOCK.parseMaterial();
    private static final Material PACKED_ICE = XMaterial.PACKED_ICE.parseMaterial();
    private static final Material FROSTED_ICE = XMaterial.FROSTED_ICE.parseMaterial();
    private static final Material BLUE_ICE = XMaterial.BLUE_ICE.parseMaterial();

    private static final Material SOUL_SAND = XMaterial.SOUL_SAND.parseMaterial();
    private static final Material HONEY_BLOCK = XMaterial.HONEY_BLOCK.parseMaterial();

    // WATER and STATIONARY_WATER on 1.12
    // WATER and BUBBLE_COLUMN on 1.13
    private static final Material water;
    private static final Material alsoWater;

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
        if (player.isGliding || player.specialFlying) return 1.0f;

        double searchBelowAmount = 0.5000001;

        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_15))
            searchBelowAmount = 1;

        Material material = player.compensatedWorld.getBukkitMaterialAt(player.lastX, player.lastY - searchBelowAmount, player.lastZ);

        return getMaterialFriction(player, material);
    }

    public static float getMaterialFriction(GrimPlayer player, Material material) {
        float friction = 0.6f;

        if (material == ICE) friction = 0.98f;
        if (material == SLIME && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) friction = 0.8f;
        // ViaVersion honey block replacement
        if (material == HONEY_BLOCK && player.getClientVersion().isOlderThan(ClientVersion.v_1_15))
            friction = 0.8f;
        if (material == PACKED_ICE) friction = 0.98f;
        if (material == FROSTED_ICE) friction = 0.98f;
        if (material == BLUE_ICE) {
            friction = 0.98f;
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) friction = 0.989f;
        }

        return friction;
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer player) {
        if (player.lastOnGround) {
            return (float) (player.speed * (0.21600002f / (f * f * f)));
        }

        // The game uses values known as flyingSpeed for some vehicles in the air
        if (player.playerVehicle != null) {
            if (player.playerVehicle.type == EntityType.PIG || player.playerVehicle instanceof PacketEntityHorse) {
                return (float) (player.speed * 0.1f);
            }

            if (player.playerVehicle instanceof PacketEntityStrider) {
                PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;
                // Vanilla multiplies by 0.1 to calculate speed
                return strider.movementSpeedAttribute * (strider.isShaking ? 0.66F : 1.0F) * 0.1f;
            }
        }

        if (player.specialFlying) {
            return player.flySpeed * 20 * (player.isSprinting ? 0.1f : 0.05f);
        }

        return player.lastSprinting ? 0.026f : 0.02f;
    }

    public static Material getOnBlock(GrimPlayer player, double x, double y, double z) {
        Material block1 = player.compensatedWorld.getBukkitMaterialAt(GrimMath.floor(x), GrimMath.floor(y - 0.2F), GrimMath.floor(z));

        if (Materials.checkFlag(block1, Materials.AIR)) {
            Material block2 = player.compensatedWorld.getBukkitMaterialAt(GrimMath.floor(x), GrimMath.floor(y - 1.2F), GrimMath.floor(z));

            if (Materials.checkFlag(block2, Materials.FENCE) || Materials.checkFlag(block2, Materials.WALL) || Materials.checkFlag(block2, Materials.GATE)) {
                return block2;
            }
        }

        return block1;
    }

    public static float getBlockSpeedFactor(GrimPlayer player) {
        if (player.isGliding || player.specialFlying) return 1.0f;
        // This system was introduces in 1.15 players to add support for honey blocks slowing players down
        if (player.getClientVersion().isOlderThan(ClientVersion.v_1_15)) return 1.0f;

        Material block = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y, player.z);

        // This is the 1.16.0 and 1.16.1 method for detecting if the player is on soul speed
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_16_1)) {
            Material onBlock = BlockProperties.getOnBlock(player, player.x, player.y, player.z);
            if (onBlock == SOUL_SAND && player.bukkitPlayer.getInventory().getBoots() != null && XMaterial.supports(16) && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
        }

        if (block == HONEY_BLOCK) return 0.4f;
        if (block == SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // 1.15- players obviously do not get this boost
            // This new method for detecting soul speed was added in 1.16.2
            if (player.bukkitPlayer.getInventory().getBoots() != null && XMaterial.supports(16) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16_2) && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
            return 0.4f;
        }

        float f = 1.0f;

        if (block == water || block == alsoWater) {
            return f;
        }

        Material block2 = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y - 0.5000001, player.z);
        if (block2 == HONEY_BLOCK) return 0.4f;
        if (block2 == SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // This new method for detecting soul speed was added in 1.16.2
            if (player.bukkitPlayer.getInventory().getBoots() != null && XMaterial.supports(16) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16_2) && player.bukkitPlayer.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0)
                return 1.0f;
            return 0.4f;
        }
        return 1.0f;
    }
}
