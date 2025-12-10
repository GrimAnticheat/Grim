package ac.grim.grimac.platform.fabric.mc1171.entity;

import ac.grim.grimac.platform.fabric.mc1161.entity.Fabric1161GrimEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Fabric1170GrimEntity extends Fabric1161GrimEntity {

    public Fabric1170GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public boolean isDead() {
        return this.entity instanceof LivingEntity living ? living.isDeadOrDying() : this.entity.isRemoved();
    }
}
