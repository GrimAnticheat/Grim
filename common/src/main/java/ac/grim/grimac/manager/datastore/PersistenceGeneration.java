package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.DataStoreMetrics;
import ac.grim.grimac.api.storage.DeletionReport;
import ac.grim.grimac.api.storage.category.Category;
import ac.grim.grimac.api.storage.kind.Operation;
import ac.grim.grimac.api.storage.query.DeleteCriteria;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Query;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * A write-service handle for one storage startup. Retired handles never submit
 * again, even when the underlying datastore acquires a new ownership lease.
 * The lock covers only the nonblocking ring submission, never database I/O.
 */
final class PersistenceGeneration implements DataStore {
    private final DataStore store;
    private boolean retired;

    PersistenceGeneration(DataStore store) {
        this.store = store;
    }

    synchronized void retire() {
        retired = true;
    }

    @Override
    public synchronized <E> void submit(Category<E> category, Consumer<E> configurer) {
        if (!retired) store.submit(category, configurer);
    }

    // These handles belong to the live event producers. Administrative writes
    // use the lifecycle's DataStore directly, not a session generation.
    @Override
    public <R> CompletionStage<R> execute(Operation<R> operation) {
        throw new UnsupportedOperationException("session generation only supports event submission");
    }

    @Override
    public <E> CompletionStage<Void> delete(Category<E> category, DeleteCriteria criteria) {
        throw new UnsupportedOperationException("session generation does not own administrative writes");
    }

    @Override
    public CompletionStage<DeletionReport> forgetPlayer(UUID player) {
        throw new UnsupportedOperationException("session generation does not own administrative writes");
    }

    @Override
    public <R> CompletionStage<Page<R>> query(Category<?> category, Query<R> query) {
        return store.query(category, query);
    }

    @Override
    public CompletionStage<Long> countViolationsInSession(UUID session) {
        return store.countViolationsInSession(session);
    }

    @Override
    public CompletionStage<Long> countUniqueChecksInSession(UUID session) {
        return store.countUniqueChecksInSession(session);
    }

    @Override
    public CompletionStage<Long> countSessionsByPlayer(UUID player) {
        return store.countSessionsByPlayer(player);
    }

    @Override
    public DataStoreMetrics metrics() {
        return store.metrics();
    }

    @Override
    public void flushAndClose(long timeoutMs) {
        retire();
    }
}
