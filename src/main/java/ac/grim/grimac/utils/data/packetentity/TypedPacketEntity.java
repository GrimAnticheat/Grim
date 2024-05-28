package ac.grim.grimac.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public abstract class TypedPacketEntity {

    private final EntityType type;
    private final boolean isLiving, isSize, isMinecart, isHorse, isAgeable, isAnimal;

    public TypedPacketEntity(EntityType type) {
        this.type = type;
        this.isLiving = EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY);
        this.isSize = type == EntityTypes.PHANTOM || type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE;
        this.isMinecart = EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
        this.isHorse = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE);
        this.isAgeable = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_AGEABLE);
        this.isAnimal = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ANIMAL);
    }

    public boolean isLivingEntity() {
        return isLiving;
    }

    public boolean isSize() {
        return isSize;
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

    public EntityType getType() {
        return type;
    }
}
