package ac.grim.grimac.platform.fabric.mc1171.entity;

import ac.grim.grimac.platform.fabric.mc1161.entity.Fabric1161GrimEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;


public class Fabric1170GrimEntity extends Fabric1161GrimEntity {

    public Fabric1170GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public boolean isDead() {
        if (this.entity instanceof LivingEntity)
            return ((LivingEntity) entity).isDead();
        return this.entity.isRemoved();
    }
}
