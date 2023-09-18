package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

public class BlockProperties {
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


    /**
     * This is used for falling onto a block (We care if there is a bouncy block)
     * This is also used for striders checking if they are on lava
     * <p>
     * For soul speed (server-sided only)
     * (we don't account for this and instead remove this debuff) And powder snow block attribute
     */
    public static StateType getOnPos(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_19_4)) {
            return BlockProperties.getOnBlock(player, playerPos.getX(), playerPos.getY(), playerPos.getZ());
        }

        Vector3i pos = getOnPos(player, playerPos, mainSupportingBlockData, 0.2F);
        return player.compensatedWorld.getStateTypeAt(pos.x, pos.y, pos.z);
    }

    public static float getFriction(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_19_4)) {
            double searchBelowAmount = 0.5000001;

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15))
                searchBelowAmount = 1;

            StateType type = player.compensatedWorld.getStateTypeAt(playerPos.getX(), playerPos.getY() - searchBelowAmount, playerPos.getZ());
            return getMaterialFriction(player, type);
        }

        StateType underPlayer = getBlockPosBelowThatAffectsMyMovement(player, mainSupportingBlockData, playerPos);
        return getMaterialFriction(player, underPlayer);
    }

    public static float getBlockSpeedFactor(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        // This system was introduces in 1.15 players to add support for honey blocks slowing players down
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) return 1.0f;
        if (player.isGliding || player.isFlying) return 1.0f;

        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_19_4)) {
            return getBlockSpeedFactorLegacy(player, playerPos);
        }

        WrappedBlockState inBlock = player.compensatedWorld.getWrappedBlockStateAt(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        float inBlockSpeedFactor = getBlockSpeedFactor(player, inBlock.getType());
        if (inBlockSpeedFactor != 1.0f || inBlock.getType() == StateTypes.WATER || inBlock.getType() == StateTypes.BUBBLE_COLUMN) return inBlockSpeedFactor;

        StateType underPlayer = getBlockPosBelowThatAffectsMyMovement(player, mainSupportingBlockData, playerPos);
        return getBlockSpeedFactor(player, underPlayer);
    }

    public static boolean onHoneyBlock(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) return false;

        StateType inBlock = player.compensatedWorld.getStateTypeAt(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        return inBlock == StateTypes.HONEY_BLOCK || getOnPos(player, mainSupportingBlockData, playerPos) == StateTypes.HONEY_BLOCK;
    }

    /**
     * Friction
     * Block jump factor
     * Block speed factor
     * <p>
     * On soul speed block (server-sided only)
     */
    private static StateType getBlockPosBelowThatAffectsMyMovement(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        Vector3i pos = getOnPos(player, playerPos, mainSupportingBlockData, 0.500001F);
        return player.compensatedWorld.getStateTypeAt(pos.x, pos.y, pos.z);
    }

    private static Vector3i getOnPos(GrimPlayer player, Vector3d playerPos, MainSupportingBlockData mainSupportingBlockData, float searchBelowPlayer) {
        Vector3i mainBlockPos = mainSupportingBlockData.getBlockPos();
        if (mainBlockPos != null) {
            StateType blockstate = player.compensatedWorld.getStateTypeAt(mainBlockPos.x, mainBlockPos.y, mainBlockPos.z);

            // I genuinely don't understand this code, or why fences are special
            boolean shouldReturn = (!((double)searchBelowPlayer <= 0.5D) || !BlockTags.FENCES.contains(blockstate)) &&
                    !BlockTags.WALLS.contains(blockstate) &&
                    !BlockTags.FENCE_GATES.contains(blockstate);

            return shouldReturn ? mainBlockPos.withY(GrimMath.floor(playerPos.getY() - (double) searchBelowPlayer)) : mainBlockPos;
        } else {
            return new Vector3i(GrimMath.floor(playerPos.getX()), GrimMath.floor(playerPos.getY() - searchBelowPlayer), GrimMath.floor(playerPos.getZ()));
        }
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

    private static StateType getOnBlock(GrimPlayer player, double x, double y, double z) {
        StateType block1 = player.compensatedWorld.getStateTypeAt(GrimMath.floor(x), GrimMath.floor(y - 0.2F), GrimMath.floor(z));

        if (block1.isAir()) {
            StateType block2 = player.compensatedWorld.getStateTypeAt(GrimMath.floor(x), GrimMath.floor(y - 1.2F), GrimMath.floor(z));

            if (Materials.isFence(block2) || Materials.isWall(block2) || Materials.isGate(block2)) {
                return block2;
            }
        }

        return block1;
    }

    private static float getBlockSpeedFactorLegacy(GrimPlayer player, Vector3d pos) {
        StateType block = player.compensatedWorld.getStateTypeAt(pos.getX(), pos.getY(), pos.getZ());

        // This is the 1.16.0 and 1.16.1 method for detecting if the player is on soul speed
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_1)) {
            StateType onBlock = BlockProperties.getOnBlock(player, pos.getX(), pos.getY(), pos.getZ());
            if (onBlock == StateTypes.SOUL_SAND && player.getInventory().getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)
                return 1.0f;
        }

        float speed = getBlockSpeedFactor(player, block);
        if (speed != 1.0f || block == StateTypes.SOUL_SAND || block == StateTypes.WATER || block == StateTypes.BUBBLE_COLUMN) return speed;

        StateType block2 = player.compensatedWorld.getStateTypeAt(pos.getX(), pos.getY() - 0.5000001, pos.getZ());
        return getBlockSpeedFactor(player, block2);
    }

    private static float getBlockSpeedFactor(GrimPlayer player, StateType type) {
        if (type == StateTypes.HONEY_BLOCK) return 0.4f;
        if (type == StateTypes.SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // This new method for detecting soul speed was added in 1.16.2
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) && player.getInventory().getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)
                return 1.0f;
            return 0.4f;
        }
        return 1.0f;
    }
}
