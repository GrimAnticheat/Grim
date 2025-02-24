package ac.grim.grimac.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public abstract class TypedPacketEntity {

    private final EntityType type;
    private final boolean isLiving, isMinecart, isHorse, isAgeable, isAnimal, isBoat;

    public TypedPacketEntity(EntityType type) {
        this.type = type;
        this.isLiving = EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY);
        this.isMinecart = EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
        this.isHorse = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE);
        this.isAgeable = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_AGEABLE);
        this.isAnimal = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ANIMAL);
        this.isBoat = EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT);
    }

    public boolean isLivingEntity() {
        return isLiving;
    }

    public boolean isMinecart() {
        return isMinecart;
    }

    public boolean isHorse() {
        return isHorse;
    }

    public boolean isAgeable() {
        return isAgeable;
    }

    public boolean isAnimal() {
        return isAnimal;
    }

    public boolean isBoat() {
        return isBoat;
    }

    public boolean isPushable() {
        // Players can only push living entities
        // Minecarts and boats are the only non-living that can push
        // Bats, parrots, and armor stands cannot
        if (type == EntityTypes.ARMOR_STAND || type == EntityTypes.BAT || type == EntityTypes.PARROT) return false;
        return isLiving || isBoat || isMinecart;
    }

    public EntityType getType() {
        return type;
    }
}
