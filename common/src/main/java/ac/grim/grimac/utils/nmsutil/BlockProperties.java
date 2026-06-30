package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.MainSupportingBlockData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityNautilus;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BlockProperties {
    public static float getFrictionInfluencedSpeed(float blockFriction, GrimPlayer player) {
        float movementSpeed = (float) player.speed;

        if (player.lastOnGround) {
            blockFriction = getModifiedFriction(blockFriction, player);

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
                float friction = blockFriction * 0.91F;
                float acceleration = player.getClientVersion().isOlderThan(ClientVersion.V_1_13) ? 0.16277136F : 0.16277137F;
                return movementSpeed * (acceleration / (friction * friction * friction));
            }

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_2) && blockFriction <= 0.6F) {
                return movementSpeed;
            }

            return movementSpeed * (0.21600002f / (blockFriction * blockFriction * blockFriction));
        }

        // The game uses values known as flyingSpeed for some vehicles in the air
        if (player.inVehicle()) {
            PacketEntity riding = player.compensatedEntities.self.getRiding();
            if (riding.getType() == EntityTypes.PIG || riding instanceof PacketEntityNautilus || riding instanceof PacketEntityHorse) {
                return movementSpeed * 0.1f;
            }

            if (riding instanceof PacketEntityStrider strider) {
                // Unsure which version the speed changed in
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20)) {
                    return movementSpeed * 0.1f;
                }

                // Vanilla multiplies by 0.1 to calculate speed
                return (float) strider.getAttributeValue(Attributes.MOVEMENT_SPEED) * (strider.isShaking ? 0.66F : 1.0F) * 0.1f;
            }
        }

        if (player.isFlying) {
            return player.flySpeed * 20 * (player.isSprinting ? 0.1f : 0.05f);
        }

        // In 1.19.4, air sprinting is based on current sprinting, not last sprinting
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_4)) {
            return player.isSprinting ? 0.025999999F : 0.02f;
        }

        return player.lastSprintingForSpeed ? player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2) ? (0.02f + 0.006f) : (float) ((double) 0.02f + 0.005999999865889549D) : 0.02f;
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
        return player.compensatedWorld.getBlockType(pos.x, pos.y, pos.z);
    }

    public static float getFriction(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_19_4)) {
            double searchBelowAmount = 0.5000001;

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15))
                searchBelowAmount = 1;

            StateType type = player.compensatedWorld.getBlockType(playerPos.getX(), playerPos.getY() - searchBelowAmount, playerPos.getZ());
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

        WrappedBlockState inBlock = player.compensatedWorld.getBlock(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        float inBlockSpeedFactor = getBlockSpeedFactor(player, inBlock.getType());
        if (inBlockSpeedFactor != 1.0f || inBlock.getType() == StateTypes.WATER || inBlock.getType() == StateTypes.BUBBLE_COLUMN) {
            return getModernVelocityMultiplier(player, inBlockSpeedFactor);
        }

        StateType underPlayer = getBlockPosBelowThatAffectsMyMovement(player, mainSupportingBlockData, playerPos);
        return getModernVelocityMultiplier(player, getBlockSpeedFactor(player, underPlayer));
    }

    public static boolean onHoneyBlock(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) return false;

        StateType inBlock = player.compensatedWorld.getBlockType(playerPos.getX(), playerPos.getY(), playerPos.getZ());
        return inBlock == StateTypes.HONEY_BLOCK || getBlockPosBelowThatAffectsMyMovement(player, mainSupportingBlockData, playerPos) == StateTypes.HONEY_BLOCK;
    }

    /**
     * Friction
     * Block jump factor
     * Block speed factor
     * <p>
     * On soul speed block (server-sided only)
     */
    private static StateType getBlockPosBelowThatAffectsMyMovement(GrimPlayer player, MainSupportingBlockData mainSupportingBlockData, Vector3d playerPos) {
        Vector3i pos = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_19_4)
                ? new Vector3i(GrimMath.floor(playerPos.getX()), GrimMath.floor(playerPos.getY() - 0.5000001), GrimMath.floor(playerPos.getZ()))
                : getOnPos(player, playerPos, mainSupportingBlockData, 0.500001F);
        return player.compensatedWorld.getBlockType(pos.x, pos.y, pos.z);
    }

    private static Vector3i getOnPos(GrimPlayer player, Vector3d playerPos, MainSupportingBlockData mainSupportingBlockData, float searchBelowPlayer) {
        Vector3i mainBlockPos = mainSupportingBlockData.blockPos();
        if (mainBlockPos != null) {
            StateType blockstate = player.compensatedWorld.getBlockType(mainBlockPos.x, mainBlockPos.y, mainBlockPos.z);

            // I genuinely don't understand this code, or why fences are special
            boolean shouldReturn = (!((double) searchBelowPlayer <= 0.5D) || !BlockTags.FENCES.contains(blockstate)) &&
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
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13))
                friction = 0.989f;
        }

        return friction;
    }

    private static StateType getOnBlock(GrimPlayer player, double x, double y, double z) {
        StateType block1 = player.compensatedWorld.getBlockType(GrimMath.floor(x), GrimMath.floor(y - 0.2F), GrimMath.floor(z));

        if (block1.isAir()) {
            StateType block2 = player.compensatedWorld.getBlockType(GrimMath.floor(x), GrimMath.floor(y - 1.2F), GrimMath.floor(z));

            if (Materials.isFence(block2) || Materials.isWall(block2) || Materials.isGate(block2)) {
                return block2;
            }
        }

        return block1;
    }

    private static float getBlockSpeedFactorLegacy(GrimPlayer player, Vector3d pos) {
        StateType block = player.compensatedWorld.getBlockType(pos.getX(), pos.getY(), pos.getZ());

        // This is the 1.16.0 and 1.16.1 method for detecting if the player is on soul speed
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_1)) {
            StateType onBlock = BlockProperties.getOnBlock(player, pos.getX(), pos.getY(), pos.getZ());
            if (onBlock == StateTypes.SOUL_SAND && player.inventory.getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED) > 0)
                return 1.0f;
        }

        float speed = getBlockSpeedFactor(player, block);
        if (speed != 1.0f || block == StateTypes.SOUL_SAND || block == StateTypes.WATER || block == StateTypes.BUBBLE_COLUMN)
            return speed;

        StateType block2 = player.compensatedWorld.getBlockType(pos.getX(), pos.getY() - 0.5000001, pos.getZ());
        return getBlockSpeedFactor(player, block2);
    }

    private static float getBlockSpeedFactor(GrimPlayer player, StateType type) {
        if (type == StateTypes.HONEY_BLOCK) return 0.4f;
        if (type == StateTypes.SOUL_SAND) {
            // Soul speed is a 1.16+ enchantment
            // This new method for detecting soul speed was added in 1.16.2
            // On 1.21, let attributes handle this
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21)
                    && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2)
                    && player.inventory.getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED) > 0)
                return 1.0f;
            return 0.4f;
        }
        return 1.0f;
    }

    private static float getModernVelocityMultiplier(GrimPlayer player, float blockSpeedFactor) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21)) return blockSpeedFactor;
        return (float) GrimMath.lerp((float) player.compensatedEntities.self.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), blockSpeedFactor, 1.0F);
    }

    public static float getModifiedFriction(float friction, GrimPlayer player) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2)) {
            return friction;
        }

        PacketEntity entity = player.inVehicle() ? player.compensatedEntities.self.getRiding() : player.compensatedEntities.self;
        return GrimMath.clamp(1.0F - (1.0F - friction) * (float) entity.getAttributeValue(Attributes.FRICTION_MODIFIER), 0.0F, 1.0F);
    }

    public static float getModifiedAirDrag(float drag, GrimPlayer player) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2)) {
            return drag;
        }

        PacketEntity entity = player.inVehicle() ? player.compensatedEntities.self.getRiding() : player.compensatedEntities.self;
        return GrimMath.clamp(1.0F - (1.0F - drag) * (float) entity.getAttributeValue(Attributes.AIR_DRAG_MODIFIER), 0.0F, 1.0F);
    }

    public static float getEntityBounciness(GrimPlayer player) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2)) {
            return 0.0F;
        }

        PacketEntity entity = player.inVehicle() ? player.compensatedEntities.self.getRiding() : player.compensatedEntities.self;
        if (entity == player.compensatedEntities.self && player.isSneaking) {
            return 0.0F;
        }

        if (!entity.isLivingEntity) {
            return 0.0F;
        }

        return (float) entity.getAttributeValue(Attributes.BOUNCINESS);
    }

    public static double getVelocityAfterHorizontalCollision(GrimPlayer player, double velocity) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2)) {
            return 0.0;
        }

        return -velocity * getEntityBounciness(player);
    }

    public static double getVelocityAfterVerticalCollision(GrimPlayer player, double velocity, double movementY) {
        return getVelocityAfterVerticalCollision(player, velocity, movementY, getEntityBounciness(player));
    }

    public static double getVelocityAfterVerticalCollision(GrimPlayer player, double velocity, double movementY, double restitution) {
        double gravity = getEffectiveGravity(player, velocity);
        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2) || velocity < 0.0 && -velocity < gravity) {
            return 0.0;
        }

        if (restitution <= 0.0F) {
            return 0.0;
        }

        double portionWithMovement = velocity == 0.0 ? 0.0 : movementY / velocity;
        double gravityCompensation = portionWithMovement * gravity;
        double effectiveDrag = GrimMath.lerp(portionWithMovement, 1.0, getModifiedAirDrag(0.98F, player));
        return (gravityCompensation - velocity) * effectiveDrag * restitution;
    }

    public static double getEffectiveGravity(GrimPlayer player, double deltaMovementY) {
        PacketEntity entity = player.inVehicle() ? player.compensatedEntities.self.getRiding() : player.compensatedEntities.self;
        double gravity = entity.getAttributeValue(Attributes.GRAVITY);

        if (deltaMovementY <= 0.0 && player.compensatedEntities.getSlowFallingAmplifier().isPresent()) {
            return player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5) ? 0.01 : Math.min(gravity, 0.01);
        }

        return gravity;
    }

    public static float getBlockBounceRestitution(StateType type, GrimPlayer player) {
        if (type == StateTypes.SLIME_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
            return 1.0F;
        }

        if (BlockTags.BEDS.contains(type) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_2) ? 0.75F : 0.66F;
        }

        if (type == StateTypes.HONEY_BLOCK
                && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14_4)
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
            return 1.0F;
        }

        return 0.0F;
    }

    public static double getBlockCollisionHeight(GrimPlayer player, WrappedBlockState block) {
        StateType type = block.getType();
        if (type.isAir()) {
            return 0D;
        }

        CollisionBox movementCollisionBox = CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), block);
        SimpleCollisionBox[] movementCollisionBoxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
        int size = movementCollisionBox.downCast(movementCollisionBoxes);

        double height = 0D;
        for (int i = 0; i < size; i++) {
            height = Math.max(height, movementCollisionBoxes[i].maxY);
        }

        return height;
    }

}
