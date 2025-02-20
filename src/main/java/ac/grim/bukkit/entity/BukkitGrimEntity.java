package ac.grim.bukkit.entity;

import ac.grim.bukkit.utils.convert.ConvertLocation;
import ac.grim.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.entity.GrimEntity;
import ac.grim.grimac.world.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitGrimEntity implements GrimEntity {

    private final Entity entity;

    public BukkitGrimEntity(Entity entity) {
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
        org.bukkit.Location bLoc = ConvertLocation.toBukkit(location);
        return PaperUtils.teleportAsync(this.entity, bLoc);
    }
}
