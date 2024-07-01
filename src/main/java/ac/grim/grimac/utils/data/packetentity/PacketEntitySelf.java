package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.checks.impl.movement.NoSlowE;
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

    public double getBlockInteractRange() {
        // Server versions older than 1.20.5 don't send the attribute, if the player is in creative then assume legacy max reach distance.
        // Or if they are on a client version older than 1.20.5.
        if (player.gamemode == GameMode.CREATIVE
                && (player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5)
                    || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_20_5))) {
            return 5.0;
        }
        return attributeMap.get(Attributes.PLAYER_BLOCK_INTERACTION_RANGE).get();
    }

    public PacketEntitySelf(GrimPlayer player) {
        super(EntityTypes.PLAYER);
        this.player = player;
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            getAttribute(Attributes.GENERIC_STEP_HEIGHT).override(0.5f);
        }

        final ValuedAttribute movementSpeed = ValuedAttribute.ranged(Attributes.GENERIC_MOVEMENT_SPEED, 0.1f, 0, 1024);
        movementSpeed.with(new WrapperPlayServerUpdateAttributes.Property("MOVEMENT_SPEED", 0.1f, new ArrayList<>()));
        attributeMap.put(Attributes.GENERIC_MOVEMENT_SPEED, movementSpeed);
        attributeMap.put(Attributes.GENERIC_JUMP_STRENGTH, ValuedAttribute.ranged(Attributes.GENERIC_JUMP_STRENGTH, 0.42f, 0, 32)
                .versionedRewriter(player, ClientVersion.V_1_20_5));
        attributeMap.put(Attributes.PLAYER_BLOCK_BREAK_SPEED, ValuedAttribute.ranged(Attributes.PLAYER_BLOCK_BREAK_SPEED, 1.0, 0, 1024)
                .versionedRewriter(player, ClientVersion.V_1_20_5));
        attributeMap.put(Attributes.PLAYER_ENTITY_INTERACTION_RANGE, ValuedAttribute.ranged(Attributes.PLAYER_ENTITY_INTERACTION_RANGE, 3, 0, 64)
                .versionedRewriter(player, ClientVersion.V_1_20_5));
        attributeMap.put(Attributes.PLAYER_BLOCK_INTERACTION_RANGE, ValuedAttribute.ranged(Attributes.PLAYER_BLOCK_INTERACTION_RANGE, 4.5, 0, 64)
                .versionedRewriter(player, ClientVersion.V_1_20_5));
        attributeMap.put(Attributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, ValuedAttribute.ranged(Attributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0, 0, 1)
                .withGetRewriter(value -> {
                    // On clients < 1.21, use depth strider enchant level always
                    final double depthStrider = EnchantmentHelper.getMaximumEnchantLevel(player.getInventory(), EnchantmentTypes.DEPTH_STRIDER, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
                    if (depthStrider == 0) {
                        return 0d;
                    }

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
                .versionedRewriter(player, ClientVersion.V_1_21));
        attributeMap.put(Attributes.PLAYER_SNEAKING_SPEED, ValuedAttribute.ranged(Attributes.PLAYER_SNEAKING_SPEED, 0.3, 0, 1)
                        .withGetRewriter(value -> {
                            final int swiftSneak = player.getInventory().getLeggings().getEnchantmentLevel(EnchantmentTypes.SWIFT_SNEAK, player.getClientVersion());
                            final double clamped = GrimMath.clampFloat(0.3F + (swiftSneak * 0.15F), 0f, 1f);
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
                .versionedRewriter(player, ClientVersion.V_1_21));
    }

    public PacketEntitySelf(GrimPlayer player, PacketEntitySelf old) {
        super(EntityTypes.PLAYER);
        this.player = player;
        this.opLevel = old.opLevel;
        this.attributeMap.putAll(old.attributeMap);
    }

    public boolean inVehicle() {
        return getRiding() != null;
    }

    @Override
    public void addPotionEffect(PotionType effect, int amplifier) {
        if (effect == PotionTypes.BLINDNESS && (potionsMap == null || !potionsMap.containsKey(PotionTypes.BLINDNESS))) {
            player.checkManager.getPostPredictionCheck(NoSlowE.class).startedSprintingBeforeBlind = player.isSprinting;
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
