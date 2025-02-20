package ac.grim.grimac.entity;

import ac.grim.grimac.world.Location;

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
