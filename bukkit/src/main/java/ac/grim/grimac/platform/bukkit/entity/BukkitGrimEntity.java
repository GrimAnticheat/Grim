package ac.grim.grimac.platform.bukkit.entity;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.platform.bukkit.world.BukkitPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitGrimEntity implements GrimEntity {

    private final Entity entity;
    private BukkitPlatformWorld bukkitPlatformWorld;

    public BukkitGrimEntity(Entity entity) {
        Objects.requireNonNull(entity);
        this.entity = entity;
    }

    public Entity getBukkitEntity() {
        return this.entity;
    }

    @Override
    public UUID getUniqueId() {
        return entity.getUniqueId();
    }

    @Override
    public boolean eject() {
        return entity.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.entity, bLoc);
    }

    @Override
    @NonNull
    public Entity getNative() {
        return entity;
    }

    @Override
    public boolean isDead() {
        return this.entity.isDead();
    }

    // TODO replace with PlayerWorldChangeEvent listener instead of checking for equality for better performance
    @Override
    public PlatformWorld getWorld() {
        if (bukkitPlatformWorld == null || !bukkitPlatformWorld.getBukkitWorld().equals(entity.getWorld())) {
            bukkitPlatformWorld = new BukkitPlatformWorld(entity.getWorld());
        }

        return bukkitPlatformWorld;
    }

    @Override
    public Location getLocation() {
        org.bukkit.Location location = this.entity.getLocation();
        return new Location(
                this.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }
}
