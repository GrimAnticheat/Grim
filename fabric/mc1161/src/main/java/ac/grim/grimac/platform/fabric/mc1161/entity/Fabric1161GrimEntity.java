package ac.grim.grimac.platform.fabric.mc1161.entity;

import ac.grim.grimac.platform.fabric.entity.AbstractFabricGrimEntity;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.CompletableFuture;

public class Fabric1161GrimEntity extends AbstractFabricGrimEntity {

    public Fabric1161GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            if (entity.getEntityWorld() instanceof ServerWorld) {
                entity.teleport(
                        location.getX(),
                        location.getY(),
                        location.getZ()

                );
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean isDead() {
        if (this.entity instanceof LivingEntity)
            return ((LivingEntity) entity).isDead();
        return this.entity.removed;
    }
}
