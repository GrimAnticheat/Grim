package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.api.storage.backend.BackendV2;
import ac.grim.grimac.api.storage.backend.KindAdapter;
import ac.grim.grimac.api.storage.backend.StorageEventHandler;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.category.Capability;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.config.WritePathConfig;
import ac.grim.grimac.api.storage.event.ServerStartupEvent;
import ac.grim.grimac.api.storage.event.SessionEvent;
import ac.grim.grimac.api.storage.event.ViolationEvent;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.checks.InMemoryCheckCatalogPersistence;
import ac.grim.grimac.api.storage.instance.OwnershipClaimResult;
import ac.grim.grimac.api.storage.instance.OwnershipRenewResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipGate;
import ac.grim.grimac.api.storage.instance.ServerOwnershipMetadata;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
import ac.grim.grimac.api.storage.registry.StoreId;
import ac.grim.grimac.internal.storage.category.V2BuiltinKinds;
import ac.grim.grimac.internal.storage.core.CategoryRouter;
import ac.grim.grimac.internal.storage.core.DataStoreImpl;
import ac.grim.grimac.internal.storage.core.V2Routes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnershipReconnectTest {
    private static final Logger LOGGER = Logger.getLogger(OwnershipReconnectTest.class.getName());
    private static final StoreId STORE = StoreId.grim("server_ownership");
    private static final long TTL = 20_000L;
    private final UUID instance = UUID.randomUUID();
    private final UUID originalStartup = UUID.randomUUID();
    private final UUID originalFence = UUID.randomUUID();
    private final ServerOwnershipGate gate = new ServerOwnershipGate(true);
    private final MemoryOwnership db = new MemoryOwnership();
    private final List<ServerStartupEvent> manifests = new CopyOnWriteArrayList<>();
    private final AtomicInteger lost = new AtomicInteger();

    @Test
    void outagePastTwentySecondsPreservesLiveSessionsAndMetadata() throws Exception {
        List<SessionEvent> sessions = new CopyOnWriteArrayList<>();
        List<ViolationEvent> violations = new CopyOnWriteArrayList<>();
        BackendV2 backend = mock(BackendV2.class);
        KindAdapter adapter = mock(KindAdapter.class);
        when(backend.id()).thenReturn("test");
        when(backend.capabilities()).thenReturn(EnumSet.noneOf(Capability.class));
        when(backend.adapterFor(any())).thenReturn(Optional.of(adapter));
        when(adapter.execute(any(), any(), any())).thenAnswer(call -> {
            if (call.getArgument(2) instanceof ac.grim.grimac.api.storage.kind.ops.EntityOps.ReassignSessionOp operation) {
                assertTrue(operation.previousStartups().contains(originalStartup));
                assertEquals(db.row.ownerStartupId(), operation.startupId());
                return 1L;
            }
            throw new AssertionError("unexpected operation " + call.getArgument(2));
        });
        when(adapter.writeHandler(any(), any(), any())).thenAnswer(invocation -> {
            if (invocation.getArgument(2) == Categories.SESSION) {
                return (StorageEventHandler<SessionEvent>) (e, seq, end) -> sessions.add(
                        new SessionEvent().sessionId(e.sessionId()).startupId(e.startupId()).playerUuid(e.playerUuid())
                                .clientBrand(e.clientBrand()).clientVersion(e.clientVersion()).startedEpochMs(e.startedEpochMs()).closedAtEpochMs(e.closedAtEpochMs()));
            }
            return (StorageEventHandler<ViolationEvent>) (e, seq, end) -> violations.add(
                    new ViolationEvent().sessionId(e.sessionId()).playerUuid(e.playerUuid()).vl(e.vl()));
        });
        V2Routes routes = V2Routes.builder().register(Categories.SESSION, StoreId.grim("sessions"),
                V2BuiltinKinds.sessions(), backend)
                .register(Categories.VIOLATION, StoreId.grim("violations"), V2BuiltinKinds.violations(), backend).build();
        DataStoreImpl store = new DataStoreImpl(new CategoryRouter(Map.of()), WritePathConfig.defaults(), LOGGER)
                .withV2Routes(routes).withOwnershipGate(gate);
        store.start();
        DataStoreLifecycle lifecycle = new DataStoreLifecycle(mock(GrimPlugin.class), mock(BackendRegistry.class));
        set(lifecycle, "config", new DataStoreConfig(null, null, null, null, null, null, null, null, null, "test"));
        set(lifecycle, "dataStore", store);
        set(lifecycle, "ownershipGate", gate);
        set(lifecycle, "ownershipAdapter", db);
        set(lifecycle, "instanceId", instance);
        set(lifecycle, "startupId", originalStartup);
        CheckRegistry checks = new CheckRegistry(new InMemoryCheckCatalogPersistence());
        checks.reload();
        set(lifecycle, "checkRegistry", checks);
        AbstractCheck check = mock(AbstractCheck.class);
        when(check.getCheckName()).thenReturn("TestCheck");
        when(check.getStableKey()).thenReturn("test-check");
        set(lifecycle, "startupLiveness", new LeaseStartupLiveness(db, STORE, db::dbNowEpochMs, 30_000L));
        lifecycle.installWriteServices(routes);
        UUID player = UUID.randomUUID();
        SessionTracker originalTracker = lifecycle.sessionTracker();
        LiveWriteHooks originalHooks = lifecycle.liveWriteHooks();
        UUID sessionBeforeOutage = originalTracker.observeActivity(player, db.now, new SessionTracker.ClientMeta("test", "fabric", 765, "test"));
        assertEquals(0, store.drain(2_000));
        assertEquals(1, sessions.size());
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add)
                .withRecovery(claim -> lifecycle.restorePersistenceAfterLeaseExpiry(gate, claim)).afterRecovery(lifecycle::finishSessionRecovery);
        try {
            db.unavailable = true;
            tick(scheduler);
            assertEquals(0, db.claims);
            db.now += TTL + 1;
            gate.close("simulate-local-deadline-expiry");
            lifecycle.liveWriteHooks().recordFlag(player, check, 1.0, "during outage", db.now, SessionTracker.ClientMeta.empty());
            org.mockito.Mockito.verify(check, org.mockito.Mockito.never()).getCheckName();
            db.unavailable = false;
            tick(scheduler);

            assertTrue(gate.allowWrites());
            assertEquals(0, lost.get());
            assertNotEquals(originalStartup, db.row.ownerStartupId());
            assertNotEquals(originalFence, db.row.fence());
            assertEquals(db.row.ownerStartupId(), manifests.get(0).startupId());
            SessionTracker recovered = lifecycle.sessionTracker();
            assertSame(originalTracker, recovered);
            assertEquals(0, store.drain(2_000));
            // Quiet players are restored immediately, before any new flag.
            assertEquals(2, sessions.size());
            assertEquals(sessionBeforeOutage, sessions.get(1).sessionId());
            assertEquals("fabric", sessions.get(1).clientBrand());
            assertEquals(765, sessions.get(1).clientVersion());
            assertEquals(100_000L, sessions.get(1).startedEpochMs());
            lifecycle.liveWriteHooks().recordFlag(player, check, 1.0, "after reconnect", db.now, SessionTracker.ClientMeta.empty());
            UUID resumedSession = recovered.currentSessionId(player);
            assertNotNull(resumedSession);
            assertEquals(sessionBeforeOutage, resumedSession);
            assertEquals(0, store.drain(2_000));
            assertEquals(1, violations.size());
            assertEquals(sessionBeforeOutage, violations.get(0).sessionId());
            assertEquals(db.row.ownerStartupId(), sessions.get(1).startupId());
            originalHooks.onQuit(player, db.now + 1, SessionTracker.ClientMeta.empty());
            UUID rejoined = recovered.observeActivity(player, db.now + 2, SessionTracker.ClientMeta.empty());
            assertNotEquals(sessionBeforeOutage, rejoined, "a real disconnect still starts a new session");
            tick(scheduler);
            assertTrue(gate.allowWrites());
            assertEquals(1, db.claims);
        } finally {
            scheduler.stop();
            lifecycle.prepareForShutdown();
            store.flushAndClose(1_000);
        }
    }

    @Test
    void briefOutageRenewsWithoutChangingStartup() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add).withRecovery(c -> fail("must not recover"));
        db.unavailable = true;
        tick(scheduler);
        db.now += 5_000;
        db.unavailable = false;
        tick(scheduler);
        assertEquals(originalStartup, db.row.ownerStartupId());
        assertEquals(0, db.claims);
        assertTrue(gate.allowWrites());
    }

    @Test
    void differentOwnerIsNeverTakenOverEvenIfItsLeaseHasExpired() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add).withRecovery(c -> fail("must not recover"));
        db.row = db.snapshot(UUID.randomUUID(), UUID.randomUUID(), db.now - 1);
        tick(scheduler);
        assertEquals(0, db.claims);
        assertEquals(1, lost.get());
        assertFalse(gate.allowWrites());
        tick(scheduler);
        assertEquals(1, lost.get());
    }

    @Test
    void publishFailureRetriesSameClaimAndKeepsWritesPaused() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        OwnershipHeartbeatScheduler scheduler = scheduler(event -> {
            if (attempts.getAndIncrement() == 0) throw new IllegalStateException("DB disconnected during publish");
            manifests.add(event);
        }).withRecovery(c -> assertFalse(gate.allowWrites()));
        db.now += TTL + 1;
        tick(scheduler);
        UUID candidate = db.row.ownerStartupId();
        assertFalse(gate.allowWrites());
        tick(scheduler);
        assertTrue(gate.allowWrites());
        assertEquals(candidate, db.row.ownerStartupId());
        assertEquals(candidate, manifests.get(0).startupId());
        assertEquals(0, lost.get());
    }

    @Test
    void slowPublishCannotReopenGateWithExpiredLease() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        OwnershipHeartbeatScheduler scheduler = scheduler(event -> {
            if (attempts.getAndIncrement() == 0) db.now += TTL + 1;
            manifests.add(event);
        }).withRecovery(c -> {});
        db.now += TTL + 1;
        tick(scheduler);
        assertFalse(gate.allowWrites());
        tick(scheduler);
        assertTrue(gate.allowWrites());
        assertNotEquals(manifests.get(0).startupId(), manifests.get(1).startupId());
    }

    @Test
    void shutdownDuringRecoveryCannotReopenGate() throws Exception {
        OwnershipHeartbeatScheduler[] holder = new OwnershipHeartbeatScheduler[1];
        holder[0] = scheduler(event -> holder[0].stop()).withRecovery(c -> {});
        db.now += TTL + 1;
        tick(holder[0]);
        assertFalse(gate.allowWrites());
        int claims = db.claims;
        tick(holder[0]);
        assertEquals(claims, db.claims);
    }

    @Test
    void missingOwnershipRowCanBeRecreated() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add).withRecovery(c -> {});
        db.row = null;
        tick(scheduler);
        assertTrue(gate.allowWrites());
        assertNotEquals(originalStartup, db.row.ownerStartupId());
    }

    @Test
    void claimFailureRemainsRetryable() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add).withRecovery(c -> {});
        db.now += TTL + 1;
        db.failNextClaim = true;
        tick(scheduler);
        assertFalse(gate.allowWrites());
        tick(scheduler);
        assertTrue(gate.allowWrites());
        assertEquals(0, lost.get());
    }

    @Test
    void ownerTakingOverBetweenReadAndClaimPreventsResume() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add).withRecovery(c -> fail("must not resume"));
        db.now += TTL + 1;
        db.takeoverOnClaim = true;
        tick(scheduler);
        assertFalse(gate.allowWrites());
        assertEquals(1, lost.get());
        assertTrue(manifests.isEmpty());
    }

    @Test
    void scheduledWorkerAutomaticallyRetriesAfterTheDatabaseReturns() throws Exception {
        OwnershipHeartbeatScheduler scheduler = scheduler(manifests::add, Duration.ofMillis(10)).withRecovery(c -> {});
        db.unavailable = true;
        try {
            scheduler.start();
            assertTrue(db.unavailableObserved.await(2, TimeUnit.SECONDS));
            gate.close("simulate-local-deadline-expiry");
            db.now += TTL + 1;
            db.unavailable = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!gate.allowWrites() && System.nanoTime() < deadline) Thread.sleep(5);
            assertTrue(gate.allowWrites());
            assertNotEquals(originalStartup, db.row.ownerStartupId());
            assertEquals(0, lost.get());
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void recoveryFromBeforeReloadCannotReplaceTheNewDatastore() throws Exception {
        DataStoreLifecycle lifecycle = new DataStoreLifecycle(mock(GrimPlugin.class), mock(BackendRegistry.class));
        ServerOwnershipGate newGate = new ServerOwnershipGate(true);
        SessionTracker currentTracker = mock(SessionTracker.class);
        set(lifecycle, "ownershipGate", newGate);
        set(lifecycle, "sessionTracker", currentTracker);
        newGate.open(UUID.randomUUID(), UUID.randomUUID(), TTL, 5_000L);
        OwnershipClaimResult stale = OwnershipClaimResult.claimed(instance, UUID.randomUUID(), UUID.randomUUID(), db.now, db.now + TTL, null);

        assertThrows(IllegalStateException.class, () -> lifecycle.restorePersistenceAfterLeaseExpiry(gate, stale));
        assertSame(currentTracker, lifecycle.sessionTracker());
        assertTrue(newGate.allowWrites());
    }

    private OwnershipHeartbeatScheduler scheduler(Consumer<ServerStartupEvent> publish) {
        return scheduler(publish, Duration.ofSeconds(10));
    }

    private OwnershipHeartbeatScheduler scheduler(Consumer<ServerStartupEvent> publish, Duration interval) {
        return new OwnershipHeartbeatScheduler(db, STORE, originalStartup, instance, originalFence, "test",
                100_000L, 100_000L, null, "test", "test", () -> null, TTL, 5_000L,
                interval, gate, publish, lost::incrementAndGet, LOGGER);
    }

    private static void tick(OwnershipHeartbeatScheduler scheduler) throws Exception {
        Method method = OwnershipHeartbeatScheduler.class.getDeclaredMethod("renewTick");
        method.setAccessible(true);
        method.invoke(scheduler);
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private final class MemoryOwnership implements ServerOwnershipAdapter {
        volatile long now = 100_000L;
        volatile boolean unavailable;
        final CountDownLatch unavailableObserved = new CountDownLatch(1);
        int claims;
        boolean failNextClaim;
        boolean takeoverOnClaim;
        volatile ServerOwnershipSnapshot row = snapshot(originalStartup, originalFence, now + TTL);

        MemoryOwnership() { gate.open(originalStartup, originalFence, TTL, 5_000L); }
        ServerOwnershipSnapshot snapshot(UUID startup, UUID fence, long expires) {
            return new ServerOwnershipSnapshot(instance, startup, fence, expires, now, 0, null, "test", null, null, null);
        }
        void connected() {
            if (unavailable) {
                unavailableObserved.countDown();
                throw new IllegalStateException("database unavailable");
            }
        }
        public void ensureStore(StoreId id) {}
        public long dbNowEpochMs() { connected(); return now; }
        public Optional<ServerOwnershipSnapshot> readOwnership(StoreId id, UUID persistent) { connected(); return Optional.ofNullable(row); }
        public OwnershipClaimResult claimOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, long ttl, ServerOwnershipMetadata metadata) {
            connected();
            claims++;
            if (failNextClaim) {
                failNextClaim = false;
                throw new IllegalStateException("DB dropped claim connection");
            }
            if (takeoverOnClaim) row = snapshot(UUID.randomUUID(), UUID.randomUUID(), now + ttl);
            ServerOwnershipSnapshot previous = row;
            if (row != null && row.activeAt(now) && !(row.ownerStartupId().equals(startup) && row.fence().equals(fence))) {
                return OwnershipClaimResult.denied(persistent, startup, fence, now, row);
            }
            row = snapshot(startup, fence, now + ttl);
            return OwnershipClaimResult.claimed(persistent, startup, fence, now, now + ttl, previous);
        }
        public OwnershipRenewResult renewOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, long ttl) {
            connected();
            if (row == null || !row.activeAt(now) || !row.ownerStartupId().equals(startup) || !row.fence().equals(fence)) {
                return OwnershipRenewResult.lost(persistent, startup, fence, now);
            }
            row = snapshot(startup, fence, now + ttl);
            return OwnershipRenewResult.renewed(persistent, startup, fence, now, now + ttl);
        }
        public boolean closeOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, String reason) { return true; }
    }
}
