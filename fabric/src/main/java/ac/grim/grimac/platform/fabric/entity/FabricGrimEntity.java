package ac.grim.grimac.platform.fabric.entity;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import com.google.common.base.Preconditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FabricGrimEntity implements GrimEntity {

    protected final Entity entity;
    protected FabricPlatformWorld fabricPlatformWorld;

    public FabricGrimEntity(Entity entity) {
        Preconditions.checkArgument(entity != null);
        this.entity = entity;
    }

    @Override
    public UUID getUniqueId() {
        return entity.getUuid();
    }

    @Override
    public boolean eject() {
        if (entity.hasPassengers()) {
            entity.removeAllPassengers();
            return true;
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(
            () -> entity.teleport(
                ((FabricPlatformWorld) location.getWorld()).getFabricWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                EnumSet.noneOf(PositionFlag.class), // todo change to match paper? Do they do this?
                location.getYaw(),
                location.getPitch(),
                true // doesn't seem to be used?
            )
        );
    }

    @Override @NonNull
    public Entity getNative() {
        return this.entity;
    }

    @Override
    public boolean isDead() {
        if (this.entity instanceof LivingEntity)
            return ((LivingEntity) entity).isDead();
        return this.entity.isRemoved();
    }

    @Override
    public PlatformWorld getWorld() {
        ServerWorld currentWorld = (ServerWorld) entity.getWorld();
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
                this.entity.getYaw(),
                this.entity.getPitch()
        );
    }
}
