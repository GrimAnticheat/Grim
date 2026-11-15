package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.DataStoreMetrics;
import ac.grim.grimac.api.storage.DeletionReport;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.event.ServerStartupEvent;
import ac.grim.grimac.api.storage.kind.Operation;
import ac.grim.grimac.api.storage.kind.ops.EntityOps;
import ac.grim.grimac.api.storage.model.ServerStartupRecord;
import ac.grim.grimac.api.storage.model.SessionRecord;
import ac.grim.grimac.api.storage.query.DeleteCriteria;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Query;
import ac.grim.grimac.api.storage.registry.StoreId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class V2InstanceRegistryTest {
    private static final Logger LOGGER = Logger.getLogger(V2InstanceRegistryTest.class.getName());
    private static final long DB_NOW = 100_000L;

    private final UUID instanceId = UUID.randomUUID();
    private final List<Operation<?>> executed = new ArrayList<>();
    private final List<ServerStartupEvent> startupWrites = new ArrayList<>();
    private final LeaseStartupLiveness heartbeatLiveness =
            new LeaseStartupLiveness(null, StoreId.grim("server_ownership"), () -> DB_NOW, 30_000L);

    @Test
    void deadStartupIsRepairedWithOneSessionUpdateAndOneStartupWrite() {
        ServerStartupRecord dead = startup(1_000L, 5_000L);
        V2InstanceRegistry registry = registry(List.of(dead), 2L);

        assertEquals(2L, registry.recoverStaleStartups(UUID.randomUUID(), heartbeatLiveness));

        EntityOps.SetIfSentinelOp close = onlySessionClose();
        assertEquals(dead.startupId(), close.key());
        assertNull(close.value());
        assertEquals("last_activity", close.fromField());
        ServerStartupEvent closed = onlyStartupWrite();
        assertEquals(dead.startupId(), closed.startupId());
        assertEquals(5_000L, closed.closedAtEpochMs());
        assertEquals("stale", closed.closeReason());
    }

    @Test
    void liveAndOwnStartupsAreLeftAlone() {
        ServerStartupRecord live = startup(80_000L, 90_000L);
        ServerStartupRecord own = startup(1_000L, 5_000L);
        V2InstanceRegistry registry = registry(List.of(live, own), 0L);

        assertEquals(0L, registry.recoverStaleStartups(own.startupId(), heartbeatLiveness));

        assertEquals(List.of(), executed.stream().filter(op -> !(op instanceof EntityOps.FindByIndexOp<?>)).toList());
        assertEquals(List.of(), startupWrites);
    }

    @Test
    void gracefulShutdownClosesEveryOpenSessionAtTheShutdownTime() {
        ServerStartupRecord current = startup(1_000L, 5_000L);
        V2InstanceRegistry registry = registry(List.of(current), 3L);

        assertEquals(3L, registry.closeCurrentStartup(current.startupId(), 9_000L));

        EntityOps.SetIfSentinelOp close = onlySessionClose();
        assertEquals(current.startupId(), close.key());
        assertEquals(9_000L, close.value());
        assertNull(close.fromField());
        ServerStartupEvent closed = onlyStartupWrite();
        assertEquals(9_000L, closed.closedAtEpochMs());
        assertEquals("graceful", closed.closeReason());
    }

    private V2InstanceRegistry registry(List<ServerStartupRecord> openStartups, long sessionsChanged) {
        DataStore store = new RecordingStore(openStartups, sessionsChanged);
        return new V2InstanceRegistry(store, (event, sequence, endOfBatch) -> startupWrites.add(event), LOGGER);
    }

    private ServerStartupRecord startup(long startedEpochMs, long lastHeartbeatEpochMs) {
        return new ServerStartupRecord(UUID.randomUUID(), instanceId, "test", null, null, null,
                startedEpochMs, lastHeartbeatEpochMs, ServerStartupRecord.OPEN, null, null);
    }

    private EntityOps.SetIfSentinelOp onlySessionClose() {
        List<EntityOps.SetIfSentinelOp> closes = executed.stream()
                .filter(EntityOps.SetIfSentinelOp.class::isInstance)
                .map(EntityOps.SetIfSentinelOp.class::cast)
                .toList();
        assertEquals(1, closes.size());
        EntityOps.SetIfSentinelOp close = closes.get(0);
        assertSame(Categories.SESSION, close.category());
        assertEquals("by_startup_open", close.indexName());
        assertEquals("closed_at", close.field());
        assertEquals(SessionRecord.OPEN, close.sentinel());
        return close;
    }

    private ServerStartupEvent onlyStartupWrite() {
        assertEquals(1, startupWrites.size());
        return startupWrites.get(0);
    }

    /** Answers the three operations the registry issues and records every one of them in {@link #executed}. */
    @SuppressWarnings("removal")
    private final class RecordingStore implements DataStore {
        private final List<ServerStartupRecord> openStartups;
        private final long sessionsChanged;

        RecordingStore(List<ServerStartupRecord> openStartups, long sessionsChanged) {
            this.openStartups = openStartups;
            this.sessionsChanged = sessionsChanged;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> CompletionStage<R> execute(Operation<R> op) {
            executed.add(op);
            Object result = new Page<>(openStartups, null);
            if (op instanceof EntityOps.SetIfSentinelOp) result = sessionsChanged;
            if (op instanceof EntityOps.GetByIdOp<?, ?> get) {
                result = openStartups.stream().filter(s -> s.startupId().equals(get.id())).findFirst();
            }
            return completedFuture((R) result);
        }

        @Override public <E> void submit(Category<E> cat, Consumer<E> configurer) { throw unsupported(); }
        @Override public <R> CompletionStage<Page<R>> query(Category<?> cat, Query<R> query) { throw unsupported(); }
        @Override public <E> CompletionStage<Void> delete(Category<E> cat, DeleteCriteria by) { throw unsupported(); }
        @Override public CompletionStage<DeletionReport> forgetPlayer(UUID uuid) { throw unsupported(); }
        @Override public CompletionStage<Long> countViolationsInSession(UUID sessionId) { throw unsupported(); }
        @Override public CompletionStage<Long> countUniqueChecksInSession(UUID sessionId) { throw unsupported(); }
        @Override public CompletionStage<Long> countSessionsByPlayer(UUID player) { throw unsupported(); }
        @Override public DataStoreMetrics metrics() { throw unsupported(); }
        @Override public void flushAndClose(long drainTimeoutMs) { throw unsupported(); }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException();
        }
    }
}
