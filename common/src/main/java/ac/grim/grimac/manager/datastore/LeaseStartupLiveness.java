package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
import ac.grim.grimac.api.storage.model.ServerStartupRecord;
import ac.grim.grimac.api.storage.registry.StoreId;
import ac.grim.grimac.internal.storage.instance.StartupLiveness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * A startup is alive while the ownership lease of its instance still names it and has not expired.
 * Without a lease source (ownership not enforced) the startup row's heartbeat age decides instead.
 * Ownership rows and the database clock are cached for a few seconds so one history page or one
 * sweep reads each instance once.
 */
final class LeaseStartupLiveness implements StartupLiveness {

    private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final @Nullable ServerOwnershipAdapter ownership;
    private final @NotNull StoreId ownershipStore;
    private final @NotNull LongSupplier dbClock;
    private final long staleStartupTtlMs;
    private final Map<UUID, Cached<ServerOwnershipSnapshot>> leases = new ConcurrentHashMap<>();
    private volatile @Nullable Cached<Long> dbNow;

    LeaseStartupLiveness(
            @Nullable ServerOwnershipAdapter ownership,
            @NotNull StoreId ownershipStore,
            @NotNull LongSupplier dbClock,
            long staleStartupTtlMs) {
        this.ownership = ownership;
        this.ownershipStore = ownershipStore;
        this.dbClock = dbClock;
        this.staleStartupTtlMs = staleStartupTtlMs;
    }

    @Override
    public boolean isAlive(@NotNull ServerStartupRecord startup) {
        if (ownership == null) return dbNowEpochMs() - startup.lastHeartbeatEpochMs() <= staleStartupTtlMs;
        ServerOwnershipSnapshot row = ownershipOf(startup.instanceId());
        return row != null && row.ownerStartupId().equals(startup.startupId()) && row.activeAt(dbNowEpochMs());
    }

    /** When a dead startup last wrote: its last lease renew if the lease still names it, else its own newest stamp. */
    long lastSeenEpochMs(@NotNull ServerStartupRecord startup) {
        ServerOwnershipSnapshot row = ownership == null ? null : ownershipOf(startup.instanceId());
        if (row != null && row.ownerStartupId().equals(startup.startupId())) return row.lastRenewedAtEpochMs();
        return Math.max(startup.startedEpochMs(), startup.lastHeartbeatEpochMs());
    }

    private @Nullable ServerOwnershipSnapshot ownershipOf(@NotNull UUID instanceId) {
        Cached<ServerOwnershipSnapshot> cached = leases.get(instanceId);
        if (cached == null || cached.expired()) {
            cached = new Cached<>(readOwnership(instanceId));
            leases.put(instanceId, cached);
        }
        return cached.value();
    }

    private @Nullable ServerOwnershipSnapshot readOwnership(@NotNull UUID instanceId) {
        try {
            return ownership.readOwnership(ownershipStore, instanceId).orElse(null);
        } catch (BackendException e) {
            // readOwnership is checked but isAlive is not; callers treat this like any other failed store read.
            throw new RuntimeException("failed to read server ownership for instance " + instanceId, e);
        }
    }

    private long dbNowEpochMs() {
        Cached<Long> cached = dbNow;
        if (cached == null || cached.expired()) {
            cached = new Cached<>(dbClock.getAsLong());
            dbNow = cached;
        }
        return cached.value();
    }

    private record Cached<T>(@Nullable T value, long readAtNanos) {
        Cached(@Nullable T value) {
            this(value, System.nanoTime());
        }

        boolean expired() {
            return System.nanoTime() - readAtNanos > CACHE_TTL_NANOS;
        }
    }
}
