package ac.grim.grimac.platform.fabric.mc1161.entity;

import ac.grim.grimac.platform.fabric.entity.AbstractFabricGrimEntity;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Fabric1161GrimEntity extends AbstractFabricGrimEntity {

    public Fabric1161GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            if (!(entity.level() instanceof ServerLevel currentLevel)) {
                return false;
            }
            PlatformWorld targetWorld = location.getWorld();
            // 1.16 path only supports same-level teleport in this adapter.
            if (targetWorld != null && targetWorld != currentLevel) {
                return false;
            }
            entity.teleportTo(
                    location.getX(),
                    location.getY(),
                    location.getZ()
            );
            return true;
        });
    }

    @Override
    public boolean isDead() {
        return entity instanceof LivingEntity living ? living.isDeadOrDying() : entity.isRemoved();
    }
}
