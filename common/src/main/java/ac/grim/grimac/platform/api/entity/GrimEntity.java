package ac.grim.grimac.platform.api.entity;

import ac.grim.grimac.utils.math.Location;

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
}
