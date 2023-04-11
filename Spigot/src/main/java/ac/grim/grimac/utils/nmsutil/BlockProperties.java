package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public class BlockProperties {

    public static float getBlockFrictionUnderPlayer(GrimPlayer player) {
        if (player.isGliding || player.isFlying) return 1.0f;

        double searchBelowAmount = 0.5000001;

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15))
            searchBelowAmount = 1;

        StateType material = player.compensatedWorld.getStateTypeAt(player.lastX, player.lastY - searchBelowAmount, player.lastZ);

        return getMaterialFriction(player, material);
    }

    public static float getMaterialFriction(GrimPlayer player, StateType material) {
        float friction = 0.6f;

        if (material == StateTypes.ICE) friction = 0.98f;
        if (material == StateTypes.SLIME_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8))
            friction = 0.8f;
        // ViaVersion honey block replacement
        if (material == StateTypes.HONEY_BLOCK && player.getClientVersion().isOlderThan(ClientVersion.V_1_15))
            friction = 0.8f;
        if (material == StateTypes.PACKED_ICE) friction = 0.98f;
        if (material == StateTypes.FROSTED_ICE) friction = 0.98f;
        if (material == StateTypes.BLUE_ICE) {
            friction = 0.98f;
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) friction = 0.989f;
        }

        return friction;
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer player) {
        if (player.lastOnGround) {
            return (float) (player.speed * (0.21600002f / (f * f * f)));
        }

        // The game uses values known as flyingSpeed for some vehicles in the air
        if (player.compensatedEntities.getSelf().getRiding() != null) {
            if (player.compensatedEntities.getSelf().getRiding().type == EntityTypes.PIG || player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityHorse) {
                return (float) (player.speed * 0.1f);
            }

            if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityStrider) {
                PacketEntityStrider strider = (PacketEntityStrider) player.compensatedEntities.getSelf().getRiding();
                // Vanilla multiplies by 0.1 to calculate speed
                return strider.movementSpeedAttribute * (strider.isShaking ? 0.66F : 1.0F) * 0.1f;
            }
        }

        if (player.isFlying) {
            return player.flySpeed * 20 * (player.isSprinting ? 0.1f : 0.05f);
        }

        // In 1.19.4, air sprinting is based on current sprinting, not last sprinting
        if (player.getClientVersion().getProtocolVersion() > ClientVersion.V_1_19_3.getProtocolVersion()) {
            return player.isSprinting ? (float) ((double) 0.02f + 0.005999999865889549D) : 0.02f;
        }

        return player.lastSprintingForSpeed ? (float) ((double) 0.02f + 0.005999999865889549D) : 0.02f;
    }

    public static StateType getOnBlock(GrimPlayer player, double x, double y, double z) {
        StateType block1 = player.compensatedWorld.getStateTypeAt(GrimMath.floor(x), GrimMath.floor(y - 0.2F), GrimMath.floor(z));

        if (block1.isAir()) {
            StateType block2 = player.compensatedWorld.getStateTypeAt(GrimMath.floor(x), GrimMath.floor(y - 1.2F), GrimMath.floor(z));

            if (Materials.isFence(block2) || Materials.isWall(block2) || Materials.isGate(block2)) {
                return block2;
            }
        }

        return block1;
    }

    public static float getBlockSpeedFactor(GrimPlayer player) {
        if (player.isGliding || player.isFlying) return 1.0f;
        // This system was introduces in 1.15 players to add support for honey blocks slowing players down
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) return 1.0f;

        StateType block = player.compensatedWorld.getStateTypeAt(player.x, player.y, player.z);

        // This is the 1.16.0 and 1.16.1 method for detecting if the player is on soul speed
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_1)) {
            StateType onBlock = BlockProperties.getOnBlock(player, player.x, player.y, player.z);
            if (onBlock == StateTypes.SOUL_SAND && player.getInventory().getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)
                return 1.0f;
        }

        if (block == StateTypes.HONEY_BLOCK) return 0.4f;
        if (block == StateTypes.SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // 1.15- players obviously do not get this boost
            // This new method for detecting soul speed was added in 1.16.2
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) && player.getInventory().getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)
                return 1.0f;
            return 0.4f;
        }

        float f = 1.0f;

        if (block == StateTypes.WATER || block == StateTypes.BUBBLE_COLUMN) {
            return f;
        }

        StateType block2 = player.compensatedWorld.getStateTypeAt(player.x, player.y - 0.5000001, player.z);
        if (block2 == StateTypes.HONEY_BLOCK) return 0.4f;
        if (block2 == StateTypes.SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // This new method for detecting soul speed was added in 1.16.2
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) && player.getInventory().getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)
                return 1.0f;
            return 0.4f;
        }
        return 1.0f;
    }
}
