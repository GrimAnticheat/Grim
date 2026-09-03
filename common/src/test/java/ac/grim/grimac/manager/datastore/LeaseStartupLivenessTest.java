package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.instance.OwnershipClaimResult;
import ac.grim.grimac.api.storage.instance.OwnershipRenewResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipMetadata;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
import ac.grim.grimac.api.storage.model.ServerStartupRecord;
import ac.grim.grimac.api.storage.registry.StoreId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaseStartupLivenessTest {
    private static final StoreId STORE = StoreId.grim("server_ownership");
    private static final long DB_NOW = 100_000L;
    private static final long STALE_TTL_MS = 30_000L;

    private final UUID instanceId = UUID.randomUUID();
    private final UUID startupId = UUID.randomUUID();
    // Heartbeat older than the stale TTL, so only the lease can say this startup is alive.
    private final ServerStartupRecord startup = startup(startupId, DB_NOW - STALE_TTL_MS - 1L);
    private final FakeOwnership ownership = new FakeOwnership();
    private final LeaseStartupLiveness liveness =
            new LeaseStartupLiveness(ownership, STORE, () -> DB_NOW, STALE_TTL_MS);

    @Test
    void aliveWhileTheLeaseNamesTheStartupAndHasNotExpired() {
        ownership.put(instanceId, lease(startupId, DB_NOW + 1L, ServerOwnershipSnapshot.OPEN, 90_000L));
        assertTrue(liveness.isAlive(startup));
    }

    @Test
    void deadWithoutAnOwnershipRow() {
        assertFalse(liveness.isAlive(startup));
    }

    @Test
    void deadWhenTheLeaseNamesAnotherStartup() {
        ownership.put(instanceId, lease(UUID.randomUUID(), DB_NOW + 1L, ServerOwnershipSnapshot.OPEN, 90_000L));
        assertFalse(liveness.isAlive(startup));
    }

    @Test
    void deadWhenTheLeaseIsClosed() {
        ownership.put(instanceId, lease(startupId, DB_NOW + 1L, 95_000L, 90_000L));
        assertFalse(liveness.isAlive(startup));
    }

    @Test
    void deadOnceTheLeaseHasExpired() {
        ownership.put(instanceId, lease(startupId, DB_NOW, ServerOwnershipSnapshot.OPEN, 90_000L));
        assertFalse(liveness.isAlive(startup));
    }

    @Test
    void heartbeatAgeDecidesWhenOwnershipIsNotEnforced() {
        LeaseStartupLiveness fallback = new LeaseStartupLiveness(null, STORE, () -> DB_NOW, STALE_TTL_MS);
        assertFalse(fallback.isAlive(startup));
        assertTrue(fallback.isAlive(startup(startupId, DB_NOW - STALE_TTL_MS)));
    }

    @Test
    void oneOwnershipReadServesEveryStartupOfTheSameInstance() {
        ownership.put(instanceId, lease(startupId, DB_NOW + 1L, ServerOwnershipSnapshot.OPEN, 90_000L));
        assertTrue(liveness.isAlive(startup));
        assertFalse(liveness.isAlive(startup(UUID.randomUUID(), DB_NOW)));
        assertFalse(liveness.isAlive(startup(UUID.randomUUID(), DB_NOW)));
        assertEquals(1, ownership.reads);
    }

    @Test
    void deadStartupWasLastSeenAtItsFinalLeaseRenew() {
        ownership.put(instanceId, lease(startupId, DB_NOW, ServerOwnershipSnapshot.OPEN, 80_000L));
        assertEquals(80_000L, liveness.lastSeenEpochMs(startup));
    }

    @Test
    void startupWithoutItsOwnLeaseWasLastSeenAtItsNewestOwnStamp() {
        ownership.put(instanceId, lease(UUID.randomUUID(), DB_NOW + 1L, ServerOwnershipSnapshot.OPEN, 90_000L));
        assertEquals(startup.lastHeartbeatEpochMs(), liveness.lastSeenEpochMs(startup));
    }

    private ServerStartupRecord startup(UUID id, long lastHeartbeatEpochMs) {
        return new ServerStartupRecord(id, instanceId, "test", null, null, null,
                lastHeartbeatEpochMs - 1_000L, lastHeartbeatEpochMs, ServerStartupRecord.OPEN, null, null);
    }

    private ServerOwnershipSnapshot lease(UUID owner, long leaseExpiresAt, long closedAt, long lastRenewedAt) {
        return new ServerOwnershipSnapshot(instanceId, owner, UUID.randomUUID(), leaseExpiresAt, lastRenewedAt,
                closedAt, null, null, null, null, null);
    }

    private static final class FakeOwnership implements ServerOwnershipAdapter {
        private final Map<UUID, ServerOwnershipSnapshot> rows = new HashMap<>();
        int reads;

        void put(UUID instanceId, ServerOwnershipSnapshot row) {
            rows.put(instanceId, row);
        }

        @Override
        public Optional<ServerOwnershipSnapshot> readOwnership(StoreId id, UUID persistentId) {
            reads++;
            return Optional.ofNullable(rows.get(persistentId));
        }

        @Override public void ensureStore(StoreId id) {}
        @Override public long dbNowEpochMs() { throw new UnsupportedOperationException(); }

        @Override
        public OwnershipClaimResult claimOwnership(
                StoreId id, UUID persistentId, UUID startupId, UUID fence, long ttlMs,
                ServerOwnershipMetadata metadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OwnershipRenewResult renewOwnership(
                StoreId id, UUID persistentId, UUID startupId, UUID fence, long ttlMs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean closeOwnership(StoreId id, UUID persistentId, UUID startupId, UUID fence, String reason) {
            throw new UnsupportedOperationException();
        }
    }
}
