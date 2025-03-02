package ac.grim.grimac.platform.fabric.mc1611.entity;

import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.entity.FabricGrimEntity;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.CompletableFuture;

public class Fabric1611GrimEntity extends FabricGrimEntity {

    public Fabric1611GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> {
            entity.teleport(
                    location.getX(),
                    location.getY(),
                    location.getZ()
            );
            return true;
        });
    }

    @Override
    public boolean isDead() {
        if (this.entity instanceof LivingEntity)
            return ((LivingEntity) entity).isDead();
        return this.entity.removed;
    }

    @Override
    public PlatformWorld getWorld() {
        ServerWorld currentWorld = (ServerWorld) entity.getEntityWorld();
        if (fabricPlatformWorld == null || fabricPlatformWorld.getFabricWorld() != currentWorld) {
            fabricPlatformWorld = new FabricPlatformWorld(currentWorld);
        }
        return fabricPlatformWorld;
    }

    @Override
    public Location getLocation() {
        return new Location(
                this.getWorld(),
                this.entity.getX(),
                this.entity.getY(),
                this.entity.getZ(),
                this.entity.yaw,
                this.entity.pitch
        );
    }
}
