package ac.grim.grimac.platform.fabric.entity;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.inject.FabricEntityHandle;
import ac.grim.grimac.utils.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractFabricGrimEntity<T> implements GrimEntity {

    protected volatile T entity;

    public AbstractFabricGrimEntity(T entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    protected void setNativeEntity(T entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    protected FabricEntityHandle entityHandle() {
        return (FabricEntityHandle) entity;
    }

    @Override
    public UUID getUniqueId() {
        return entityHandle().fabricEntityUuid();
    }

    @Override
    public boolean eject() {
        return entityHandle().fabricEjectPassengers();
    }

    @Override
    public @NotNull T getNative() {
        return this.entity;
    }

    @Override
    public PlatformWorld getWorld() {
        return (PlatformWorld) entityHandle().fabricWorld();
    }

    @Override
    public Location getLocation() {
        FabricEntityHandle handle = entityHandle();
        return new Location(
                this.getWorld(),
                handle.fabricPosX(),
                handle.fabricPosY(),
                handle.fabricPosZ(),
                handle.fabricYaw(1.0F),
                handle.fabricPitch(1.0F)
        );
    }

    @Override
    public double distanceSquared(double oX, double oY, double oZ) {
        FabricEntityHandle handle = entityHandle();
        double x = handle.fabricPosX();
        double y = handle.fabricPosY();
        double z = handle.fabricPosZ();
        double distX = (x - oX) * (x - oX);
        double distY = (y - oY) * (y - oY);
        double distZ = (z - oZ) * (z - oZ);
        return distX + distY + distZ;
    }
}
