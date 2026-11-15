package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.api.storage.backend.BackendV2;
import ac.grim.grimac.api.storage.backend.KindAdapter;
import ac.grim.grimac.api.storage.category.Capability;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.config.DataStoreConfig;
import ac.grim.grimac.api.storage.config.WritePathConfig;
import ac.grim.grimac.api.storage.instance.*;
import ac.grim.grimac.api.storage.kind.ops.EntityOps;
import ac.grim.grimac.api.storage.model.SessionRecord;
import ac.grim.grimac.api.storage.registry.StoreId;
import ac.grim.grimac.internal.storage.backend.sql.v2.SqlEntityAdapter;
import ac.grim.grimac.internal.storage.backend.sql.v2.dialect.SqliteDialect;
import ac.grim.grimac.internal.storage.category.V2BuiltinKinds;
import ac.grim.grimac.internal.storage.core.CategoryRouter;
import ac.grim.grimac.internal.storage.core.DataStoreImpl;
import ac.grim.grimac.internal.storage.core.V2Routes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoveryQuitRetryTest {
    private static final long TTL = 20_000L;
    private static final StoreId OWNERSHIP = StoreId.grim("ownership");
    private static final StoreId SESSIONS = StoreId.grim("sessions");
    private static final Logger LOG = Logger.getLogger(RecoveryQuitRetryTest.class.getName());

    @Test
    void quitSurvivesFinalRenewFailureAndSameCandidateRetry(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.FINAL_RENEW, false, false);
    }

    @Test
    void quitSurvivesFailureAfterDurablePartialHandoff(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.AFTER_HANDOFF, false, false);
    }

    @Test
    void quitSurvivesRetryWithANewCandidate(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.FINAL_RENEW, true, false);
    }

    @Test
    void retryClosesOldConnectionAndPreservesARealRejoin(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.FINAL_RENEW, false, true);
    }

    @Test
    void expiredLocalGateRetriesCompletionBeforeReturningToOrdinaryRenewal(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.COMPLETION_GATE, false, false);
    }

    @Test
    void completionExceptionClosesGateAndRemainsRetryable(@TempDir Path directory) throws Exception {
        exercise(directory, Failure.COMPLETION_EXCEPTION, false, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void exercise(Path directory, Failure failure, boolean expireCandidate, boolean rejoin) throws Exception {
        UUID instance = UUID.randomUUID();
        UUID original = UUID.randomUUID();
        UUID originalFence = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID quietPlayer = UUID.randomUUID();
        ServerOwnershipGate gate = new ServerOwnershipGate(true);
        gate.open(original, originalFence, TTL, 5_000L);
        Ownership ownership = new Ownership(instance, original, originalFence);
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + directory.resolve("sessions.sqlite"));
        SqlEntityAdapter sql = new SqlEntityAdapter(source, new SqliteDialect(), LOG);
        var kind = V2BuiltinKinds.sessions();
        sql.ensureStore(SESSIONS, kind);
        BackendV2 backend = mock(BackendV2.class);
        KindAdapter forwarding = mock(KindAdapter.class);
        when(backend.id()).thenReturn("sqlite-test");
        when(backend.capabilities()).thenReturn(EnumSet.noneOf(Capability.class));
        when(backend.adapterFor(any())).thenReturn(Optional.of(forwarding));
        when(forwarding.writeHandler(any(), any(), any())).thenAnswer(call -> sql.writeHandler(
                call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        V2Routes routes = V2Routes.builder().register(Categories.SESSION, SESSIONS, kind, backend).build();
        DataStoreImpl store = new DataStoreImpl(new CategoryRouter(Map.of()), WritePathConfig.defaults(), LOG)
                .withV2Routes(routes).withOwnershipGate(gate);
        DataStoreLifecycle lifecycle = new DataStoreLifecycle(mock(GrimPlugin.class), mock(BackendRegistry.class));
        set(lifecycle, "config", new DataStoreConfig(null, null, null, null, null, null, null, null, null, "test"));
        set(lifecycle, "dataStore", store);
        set(lifecycle, "ownershipGate", gate);
        set(lifecycle, "ownershipAdapter", ownership);
        set(lifecycle, "instanceId", instance);
        set(lifecycle, "startupId", original);
        set(lifecycle, "startupLiveness", new LeaseStartupLiveness(ownership, OWNERSHIP, ownership::dbNowEpochMs, 30_000));
        lifecycle.installWriteServices(routes);
        store.start();
        SessionTracker tracker = lifecycle.sessionTracker();
        UUID session = tracker.observeActivity(player, 70_000, meta("fabric", 765));
        UUID quietSession = tracker.observeActivity(quietPlayer, 71_000, meta("vanilla", 766));
        assertEquals(0, store.drain(2_000));
        long quitAt = ownership.now + 500;
        UUID[] rejoined = {null};
        Runnable disconnect = () -> {
            assertFalse(gate.allowWrites());
            tracker.close(player, quitAt, SessionTracker.ClientMeta.empty());
            if (rejoin) rejoined[0] = tracker.observeActivity(player, quitAt + 100, meta("new", 767));
        };
        boolean[] transferredFailure = {false};
        when(forwarding.execute(any(), any(), any())).thenAnswer(call -> {
            Object result = sql.execute(call.getArgument(0), call.getArgument(1), call.getArgument(2));
            if (failure == Failure.AFTER_HANDOFF && !transferredFailure[0]
                    && call.getArgument(2) instanceof EntityOps.ReassignSessionOp handoff
                    && handoff.sessionId().equals(session)) {
                transferredFailure[0] = true;
                disconnect.run();
                throw new BackendException("injected failure after committed handoff", null);
            }
            return result;
        });
        boolean[] completionInterrupted = {false};
        OwnershipHeartbeatScheduler scheduler = new OwnershipHeartbeatScheduler(
                ownership, OWNERSHIP, original, instance, originalFence, "test", 70_000, 70_000,
                null, null, null, () -> null, TTL, 5_000, Duration.ofSeconds(10), gate,
                event -> {}, () -> fail("ownership must not be permanently lost"), LOG)
                .withRecovery(claim -> {
                    lifecycle.restorePersistenceAfterLeaseExpiry(gate, claim);
                    if (failure != Failure.AFTER_HANDOFF && !ownership.failureInjected) ownership.failNextRenew = disconnect;
                }).afterRecovery(() -> {
                    if (!completionInterrupted[0] && (failure == Failure.COMPLETION_GATE || failure == Failure.COMPLETION_EXCEPTION)) {
                        completionInterrupted[0] = true;
                        assertTrue(ownership.row.activeAt(ownership.now), "DB lease survives the local gate deadline");
                        if (failure == Failure.COMPLETION_EXCEPTION) throw new IllegalStateException("injected completion failure");
                        gate.close("simulate-local-deadline-before-completion");
                    }
                    lifecycle.finishSessionRecovery();
                });
        try {
            tick(scheduler);
            assertFalse(gate.allowWrites(), "first recovery must fail with writes still paused");
            UUID firstCandidate = ownership.row.ownerStartupId();
            SessionRecord handedOff = sql.<Optional<SessionRecord>>execute(SESSIONS, kind,
                    new EntityOps.GetByIdOp<>(Categories.SESSION, session)).orElseThrow();
            assertEquals(firstCandidate, handedOff.startupId());
            assertEquals(0, handedOff.closedAtEpochMs(), "quit has not reached the DB yet");
            if (expireCandidate) ownership.now += TTL + 1;

            tick(scheduler);
            if (failure == Failure.COMPLETION_GATE || failure == Failure.COMPLETION_EXCEPTION) {
                assertFalse(gate.allowWrites(), "failed completion must keep normal writes paused");
                tick(scheduler);
            }
            assertTrue(gate.allowWrites());
            assertEquals(0, store.drain(2_000));
            if (!expireCandidate) assertEquals(firstCandidate, ownership.row.ownerStartupId());
            else assertNotEquals(firstCandidate, ownership.row.ownerStartupId());
            SessionRecord closed = sql.<Optional<SessionRecord>>execute(SESSIONS, kind,
                    new EntityOps.GetByIdOp<>(Categories.SESSION, session)).orElseThrow();
            assertEquals(quitAt, closed.closedAtEpochMs(), "recovery must persist the retained quit");
            assertEquals(70_000, closed.startedEpochMs());
            assertEquals("fabric", closed.clientBrand());
            assertEquals(765, closed.clientVersion());
            SessionRecord quiet = sql.<Optional<SessionRecord>>execute(SESSIONS, kind,
                    new EntityOps.GetByIdOp<>(Categories.SESSION, quietSession)).orElseThrow();
            assertFalse(quiet.isClosed());
            assertEquals(ownership.row.ownerStartupId(), quiet.startupId());
            assertEquals(quietSession, tracker.currentSessionId(quietPlayer));
            if (rejoin) {
                assertNotEquals(session, rejoined[0]);
                assertEquals(rejoined[0], tracker.currentSessionId(player));
                SessionRecord joined = sql.<Optional<SessionRecord>>execute(SESSIONS, kind,
                        new EntityOps.GetByIdOp<>(Categories.SESSION, rejoined[0])).orElseThrow();
                assertEquals("new", joined.clientBrand());
                assertEquals(767, joined.clientVersion());
                assertFalse(joined.isClosed());
            } else assertNull(tracker.currentSessionId(player));
        } finally {
            scheduler.stop();
            lifecycle.prepareForShutdown();
            store.flushAndClose(1_000);
        }
    }

    private enum Failure { FINAL_RENEW, AFTER_HANDOFF, COMPLETION_GATE, COMPLETION_EXCEPTION }

    private static SessionTracker.ClientMeta meta(String brand, int version) {
        return new SessionTracker.ClientMeta("test", brand, version, "test");
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void tick(OwnershipHeartbeatScheduler scheduler) throws Exception {
        Method tick = OwnershipHeartbeatScheduler.class.getDeclaredMethod("renewTick");
        tick.setAccessible(true);
        tick.invoke(scheduler);
    }

    private static final class Ownership implements ServerOwnershipAdapter {
        final UUID instance;
        long now = 100_000;
        ServerOwnershipSnapshot row;
        Runnable failNextRenew;
        boolean failureInjected;

        Ownership(UUID instance, UUID original, UUID fence) {
            this.instance = instance;
            row = snapshot(original, fence, now - 1);
        }

        ServerOwnershipSnapshot snapshot(UUID startup, UUID fence, long expires) {
            return new ServerOwnershipSnapshot(instance, startup, fence, expires, now, 0, null, "test", null, null, null);
        }

        public void ensureStore(StoreId id) {}
        public long dbNowEpochMs() { return now; }
        public Optional<ServerOwnershipSnapshot> readOwnership(StoreId id, UUID persistent) { return Optional.of(row); }
        public OwnershipClaimResult claimOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, long ttl, ServerOwnershipMetadata metadata) {
            ServerOwnershipSnapshot previous = row;
            row = snapshot(startup, fence, now + ttl);
            return OwnershipClaimResult.claimed(persistent, startup, fence, now, now + ttl, previous);
        }
        public OwnershipRenewResult renewOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, long ttl) {
            if (failNextRenew != null) {
                Runnable disconnect = failNextRenew;
                failNextRenew = null;
                failureInjected = true;
                disconnect.run();
                throw new IllegalStateException("injected final renewal failure after quit");
            }
            if (!row.activeAt(now) || !row.ownerStartupId().equals(startup) || !row.fence().equals(fence)) {
                return OwnershipRenewResult.lost(persistent, startup, fence, now);
            }
            row = snapshot(startup, fence, now + ttl);
            return OwnershipRenewResult.renewed(persistent, startup, fence, now, now + ttl);
        }
        public boolean closeOwnership(StoreId id, UUID persistent, UUID startup, UUID fence, String reason) { return true; }
    }
}
