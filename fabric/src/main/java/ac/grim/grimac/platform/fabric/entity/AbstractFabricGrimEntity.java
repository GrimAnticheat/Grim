package ac.grim.grimac.platform.fabric.entity;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractFabricGrimEntity implements GrimEntity {

    protected final Entity entity;

    public AbstractFabricGrimEntity(Entity entity) {
        this.entity = Objects.requireNonNull(entity);
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
    public @NotNull Entity getNative() {
        return this.entity;
    }

    @Override
    public PlatformWorld getWorld() {
        return (PlatformWorld) entity.world;
    }

    @Override
    public Location getLocation() {
        return new Location(
                this.getWorld(),
                this.entity.getX(),
                this.entity.getY(),
                this.entity.getZ(),
                this.entity.getYaw(1.0F),
                this.entity.getPitch(1.0F)
        );
    }
}
