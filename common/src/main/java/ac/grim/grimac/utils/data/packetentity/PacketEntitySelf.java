package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.checks.impl.sprint.SprintD;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import ac.grim.grimac.utils.inventory.EnchantmentHelper;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class PacketEntitySelf extends PacketEntity {

    private final GrimPlayer player;
    @Getter
    @Setter
    int opLevel;

    public PacketEntitySelf(GrimPlayer player) {
        super(player, EntityTypes.PLAYER);
        this.player = player;
    }

    public PacketEntitySelf(GrimPlayer player, PacketEntitySelf old) {
        super(player, EntityTypes.PLAYER);
        this.player = player;
        this.opLevel = old.opLevel;
        this.attributeMap.putAll(old.attributeMap);
    }

    @Override
    protected void initAttributes(GrimPlayer player) {
        super.initAttributes(player);
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            setAttribute(Attributes.STEP_HEIGHT, 0.5f);
        }

        getAttribute(Attributes.SCALE).orElseThrow().withSetRewriter((oldValue, newValue) -> {
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_20_5) || (newValue).equals(oldValue)) {
                return oldValue;
            } else {
                // Elytra, standing, sneaking (1.14)
                player.possibleEyeHeights[2][0] = 0.4 * newValue;
                player.possibleEyeHeights[2][1] = 1.62 * newValue;
                player.possibleEyeHeights[2][2] = 1.27 * newValue;

                // sneaking (1.14), standing, Elytra
                player.possibleEyeHeights[1][0] = 1.27 * newValue;
                player.possibleEyeHeights[1][1] = 1.62 * newValue;
                player.possibleEyeHeights[1][2] = 0.4 * newValue;

                // standing, sneaking (1.14), Elytra
                player.possibleEyeHeights[0][0] = 1.62 * newValue;
                player.possibleEyeHeights[0][1] = 1.27 * newValue;
                player.possibleEyeHeights[0][2] = 0.4 * newValue;
                return newValue;
            }
        });

        final ValuedAttribute movementSpeed = ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 0.1f, 0, 1024);
        movementSpeed.with(new WrapperPlayServerUpdateAttributes.Property(Attributes.MOVEMENT_SPEED, 0.1f, new ArrayList<>()));
        trackAttribute(movementSpeed);
        trackAttribute(ValuedAttribute.ranged(Attributes.ATTACK_SPEED, 4, 0, 1024)
                .requiredVersion(player, ClientVersion.V_1_9));
        trackAttribute(ValuedAttribute.ranged(Attributes.JUMP_STRENGTH, 0.42f, 0, 32)
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.BLOCK_BREAK_SPEED, 1.0, 0, 1024)
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.MINING_EFFICIENCY, 0, 0, 1024)
                .requiredVersion(player, ClientVersion.V_1_21));
        trackAttribute(ValuedAttribute.ranged(Attributes.SUBMERGED_MINING_SPEED, 0.2, 0, 20)
                .requiredVersion(player, ClientVersion.V_1_21));
        trackAttribute(ValuedAttribute.ranged(Attributes.ENTITY_INTERACTION_RANGE, 3, 0, 64)
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.BLOCK_INTERACTION_RANGE, 4.5, 0, 64)
                .withGetRewriter(value -> {
                    // Server versions older than 1.20.5 don't send the attribute, if the player is in creative then assume legacy max reach distance.
                    if (player.gamemode == GameMode.CREATIVE
                            && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_20_5)) {
                        return 5.0;
                    }
                    // < 1.20.5 is unchanged due to requiredVersion, otherwise controlled by the server
                    return value;
                })
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.WATER_MOVEMENT_EFFICIENCY, 0, 0, 1)
                .withGetRewriter(value -> {
                    // Depth strider was added in 1.8
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                        return 0d;
                    }

                    // On clients < 1.21, use depth strider enchant level always
                    final double depthStrider = EnchantmentHelper.getMaximumEnchantLevel(player.getInventory(), EnchantmentTypes.DEPTH_STRIDER, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21)) {
                        return depthStrider;
                    }

                    // Server is older than 1.21, but player is on 1.21+ so return depth strider value / 3 to simulate via
                    // https://github.com/ViaVersion/ViaVersion/blob/dc503cd613f5cf00a6f11b78e52b1a76a42acf91/common/src/main/java/com/viaversion/viaversion/protocols/v1_20_5to1_21/storage/EfficiencyAttributeStorage.java#L34
                    if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_21)) {
                        return depthStrider / 3;
                    }

                    // We are on a version that fully supports this value!
                    return value;
                })
                .requiredVersion(player, ClientVersion.V_1_21));
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_EFFICIENCY, 0, 0, 1)
                .requiredVersion(player, ClientVersion.V_1_21));
        trackAttribute(ValuedAttribute.ranged(Attributes.SNEAKING_SPEED, 0.3, 0, 1)
                .withGetRewriter(value -> {
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_19)) {
                        return (double) 0.3f;
                    }

                    final int swiftSneak = player.getInventory().getLeggings().getEnchantmentLevel(EnchantmentTypes.SWIFT_SNEAK, player.getClientVersion());
                    final double clamped = GrimMath.clamp(0.3f + swiftSneak * 0.15f, 0f, 1f);
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21)) {
                        return clamped;
                    }

                    // https://github.com/ViaVersion/ViaVersion/blob/dc503cd613f5cf00a6f11b78e52b1a76a42acf91/common/src/main/java/com/viaversion/viaversion/protocols/v1_20_5to1_21/storage/EfficiencyAttributeStorage.java#L32
                    if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_21)) {
                        return clamped;
                    }

                    // We are on a version that fully supports this value!
                    return value;
                })
                .requiredVersion(player, ClientVersion.V_1_21));
    }

    public boolean inVehicle() {
        return getRiding() != null;
    }

    @Override
    public void addPotionEffect(PotionType effect, int amplifier) {
        if (effect == PotionTypes.BLINDNESS && !hasPotionEffect(PotionTypes.BLINDNESS)) {
            player.checkManager.getPostPredictionCheck(SprintD.class).startedSprintingBeforeBlind = player.isSprinting;
        }

        player.pointThreeEstimator.updatePlayerPotions(effect, amplifier);
        super.addPotionEffect(effect, amplifier);
    }

    @Override
    public void removePotionEffect(PotionType effect) {
        player.pointThreeEstimator.updatePlayerPotions(effect, null);
        super.removePotionEffect(effect);
    }

    @Override
    public void onFirstTransaction(boolean relative, boolean hasPos, double relX, double relY, double relZ, GrimPlayer player) {
        // Player ignores this
    }

    @Override
    public void onSecondTransaction() {
        // Player ignores this
    }

    @Override
    public SimpleCollisionBox getPossibleCollisionBoxes() {
        return player.boundingBox.copy(); // Copy to retain behavior of PacketEntity
    }
}
