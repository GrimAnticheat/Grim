package ac.grim.grimac.platform.api.entity;

import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.utils.math.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GrimEntity {
    UUID getUniqueId();

    /**
     * Eject any passenger.
     *
     * @return True if there was a passenger.
     */
    boolean eject();

    CompletableFuture<Boolean> teleportAsync(Location location);

    @NonNull
    Object getNative();

    boolean isDead();

    PlatformWorld getWorld();

    Location getLocation();
}
