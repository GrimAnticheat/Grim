package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.checks.impl.movement.NoSlowE;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
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
        attributeMap.put(Attributes.GENERIC_JUMP_STRENGTH, ValuedAttribute.ranged(Attributes.GENERIC_JUMP_STRENGTH, 0.42f, 0, 32));
        attributeMap.put(Attributes.PLAYER_BLOCK_BREAK_SPEED, ValuedAttribute.ranged(Attributes.PLAYER_BLOCK_BREAK_SPEED, 1.0, 0, 1024));
        attributeMap.put(Attributes.PLAYER_ENTITY_INTERACTION_RANGE, ValuedAttribute.ranged(Attributes.PLAYER_ENTITY_INTERACTION_RANGE, 3, 0, 64));
        attributeMap.put(Attributes.PLAYER_BLOCK_INTERACTION_RANGE, ValuedAttribute.ranged(Attributes.PLAYER_BLOCK_INTERACTION_RANGE, 4.5, 0, 64));
        attributeMap.put(Attributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, ValuedAttribute.ranged(Attributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0, 0, 1));
        attributeMap.put(Attributes.PLAYER_SNEAKING_SPEED, ValuedAttribute.ranged(Attributes.PLAYER_SNEAKING_SPEED, 0.3, 0, 1));
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
