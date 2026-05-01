package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.model.SettingRecord;
import ac.grim.grimac.api.storage.model.SettingScope;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Queries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete {@link PlayerToggleStore} backed by a {@link DataStore}.
 *
 * <h2>Lock-free state machine</h2>
 * Each (player, key) holds an {@link AtomicReference} to a {@link TogglePair}
 * carrying the value and the {@link Source} that wrote it. State transitions are
 * a precedence-gated CAS loop:
 * <pre>
 *   USER_TOGGLED      30  — staff explicitly issued /grim toggle. Stomps anything.
 *   PERSISTED         20  — prefetch returned a row. Wins over PERMISSION_DEFAULT.
 *   PERMISSION_DEFAULT 10 — onUserLogin's grim.*.enable-on-join fallback.
 *   UNKNOWN            0  — initial state.
 * </pre>
 *
 * <h2>Coalesced persists</h2>
 * User toggles update the in-memory value immediately and flag the slot dirty,
 * but the DB write is deferred via a single-flight scheduled flush.
 */
public final class PlayerToggleStoreImpl implements PlayerToggleStore {

    public enum Source {
        UNKNOWN(0),
        PERMISSION_DEFAULT(10),
        PERSISTED(20),
        USER_TOGGLED(30);

        public final int precedence;
        Source(int precedence) { this.precedence = precedence; }
    }

    public record TogglePair(@NotNull Source source, @Nullable Boolean value) {
        static final TogglePair INITIAL = new TogglePair(Source.UNKNOWN, null);
    }

    /** Per-player slot. All fields are lock-free. */
    public static final class ToggleSlot {
        final AtomicReference<TogglePair> alerts = new AtomicReference<>(TogglePair.INITIAL);
        final AtomicReference<TogglePair> verbose = new AtomicReference<>(TogglePair.INITIAL);
        final AtomicReference<TogglePair> brands = new AtomicReference<>(TogglePair.INITIAL);
        final AtomicBoolean alertsDirty = new AtomicBoolean();
        final AtomicBoolean verboseDirty = new AtomicBoolean();
        final AtomicBoolean brandsDirty = new AtomicBoolean();
        /** Single-flight reference: at most one flush per slot in scheduler at a time. */
        final AtomicReference<ScheduledFuture<?>> pendingFlush = new AtomicReference<>();
    }

    private static final long DEFAULT_FLUSH_DELAY_MS = 500L;

    private final DataStore store;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    private final long flushDelayMs;
    private final boolean ownsScheduler;
    private final Map<UUID, ToggleSlot> slots = new ConcurrentHashMap<>();

    public PlayerToggleStoreImpl(@NotNull DataStore store, @NotNull Logger logger) {
        this(store, logger, defaultScheduler(), DEFAULT_FLUSH_DELAY_MS, true);
    }

    PlayerToggleStoreImpl(@NotNull DataStore store,
                          @NotNull Logger logger,
                          @NotNull ScheduledExecutorService scheduler,
                          long flushDelayMs,
                          boolean ownsScheduler) {
        this.store = store;
        this.logger = logger;
        this.scheduler = scheduler;
        this.flushDelayMs = flushDelayMs;
        this.ownsScheduler = ownsScheduler;
    }

    private static ScheduledExecutorService defaultScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grim-toggle-flush");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void prefetch(@NotNull UUID uuid) {
        ToggleSlot slot = slots.computeIfAbsent(uuid, k -> new ToggleSlot());
        prefetchKey(uuid, slot.alerts, KEY_ALERTS);
        prefetchKey(uuid, slot.verbose, KEY_VERBOSE);
        prefetchKey(uuid, slot.brands, KEY_BRANDS);
    }

    private void prefetchKey(UUID uuid, AtomicReference<TogglePair> ref, String key) {
        store.query(Categories.SETTING,
                        new Queries.GetSetting(SettingScope.PLAYER, uuid.toString(), key))
                .whenComplete((page, err) -> {
                    if (err != null) {
                        logger.log(Level.FINE, "[grim-toggle] prefetch " + key + " failed for " + uuid, err);
                        return;
                    }
                    Boolean value = decodeBool(page);
                    if (value != null) trySetWithPrecedence(ref, Source.PERSISTED, value);
                });
    }

    @Override
    public @Nullable Boolean current(@NotNull UUID uuid, @NotNull String key) {
        ToggleSlot slot = slots.get(uuid);
        if (slot == null) return null;
        AtomicReference<TogglePair> ref = refFor(slot, key);
        return ref == null ? null : ref.get().value();
    }

    @Override
    public void applyPermissionDefault(@NotNull UUID uuid, @NotNull String key, boolean value) {
        ToggleSlot slot = slots.computeIfAbsent(uuid, k -> new ToggleSlot());
        AtomicReference<TogglePair> ref = refFor(slot, key);
        if (ref == null) return;
        if (trySetWithPrecedence(ref, Source.PERMISSION_DEFAULT, value)) {
            persist(uuid, key, value);
        }
    }

    @Override
    public void applyUserToggle(@NotNull UUID uuid, @NotNull String key, boolean value) {
        ToggleSlot slot = slots.computeIfAbsent(uuid, k -> new ToggleSlot());
        AtomicReference<TogglePair> ref = refFor(slot, key);
        AtomicBoolean dirty = dirtyFor(slot, key);
        if (ref == null || dirty == null) return;
        ref.set(new TogglePair(Source.USER_TOGGLED, value));
        dirty.set(true);
        scheduleFlush(slot, uuid);
    }

    @Override
    public void evict(@NotNull UUID uuid) {
        ToggleSlot slot = slots.remove(uuid);
        if (slot == null) return;
        ScheduledFuture<?> f = slot.pendingFlush.getAndSet(null);
        if (f != null) f.cancel(false);
        flushDirty(slot, uuid);
    }

    /**
     * Precedence-gated CAS loop. Returns true on successful set, false if the
     * current source already meets or exceeds the new source's precedence.
     */
    private static boolean trySetWithPrecedence(AtomicReference<TogglePair> ref, Source newSource, Boolean newValue) {
        TogglePair next = new TogglePair(newSource, newValue);
        while (true) {
            TogglePair cur = ref.get();
            if (cur.source().precedence >= newSource.precedence) return false;
            if (ref.compareAndSet(cur, next)) return true;
        }
    }

    /**
     * Schedule a coalesced flush. Single-flight: if a flush is already queued,
     * don't queue another — the queued one will read the latest atomic values
     * at fire time and pick up everything dirty.
     */
    private void scheduleFlush(ToggleSlot slot, UUID uuid) {
        if (slot.pendingFlush.get() != null) return;
        ScheduledFuture<?> f = scheduler.schedule(() -> {
            slot.pendingFlush.set(null);
            flushDirty(slot, uuid);
        }, flushDelayMs, TimeUnit.MILLISECONDS);
        if (!slot.pendingFlush.compareAndSet(null, f)) f.cancel(false);
    }

    private void flushDirty(ToggleSlot slot, UUID uuid) {
        flushOne(uuid, KEY_ALERTS, slot.alertsDirty, slot.alerts);
        flushOne(uuid, KEY_VERBOSE, slot.verboseDirty, slot.verbose);
        flushOne(uuid, KEY_BRANDS, slot.brandsDirty, slot.brands);
    }

    private void flushOne(UUID uuid, String key, AtomicBoolean dirty, AtomicReference<TogglePair> ref) {
        if (!dirty.compareAndSet(true, false)) return;
        Boolean v = ref.get().value();
        if (v != null) persist(uuid, key, v);
    }

    private static AtomicReference<TogglePair> refFor(ToggleSlot s, String key) {
        switch (key) {
            case KEY_ALERTS: return s.alerts;
            case KEY_VERBOSE: return s.verbose;
            case KEY_BRANDS: return s.brands;
            default: return null;
        }
    }

    private static AtomicBoolean dirtyFor(ToggleSlot s, String key) {
        switch (key) {
            case KEY_ALERTS: return s.alertsDirty;
            case KEY_VERBOSE: return s.verboseDirty;
            case KEY_BRANDS: return s.brandsDirty;
            default: return null;
        }
    }

    private void persist(UUID uuid, String key, boolean value) {
        long now = System.currentTimeMillis();
        store.submit(Categories.SETTING, e -> e
                .scope(SettingScope.PLAYER)
                .scopeKey(uuid.toString())
                .key(key)
                .value(encodeBool(value))
                .updatedEpochMs(now));
    }

    private static byte[] encodeBool(boolean v) {
        return new byte[] { v ? (byte) 1 : (byte) 0 };
    }

    private static @Nullable Boolean decodeBool(@NotNull Page<SettingRecord> page) {
        if (page.items().isEmpty()) return null;
        byte[] v = page.items().get(0).value();
        if (v == null || v.length == 0) return null;
        if (v.length == 1) return v[0] != 0;
        String s = new String(v, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        return s.equals("1") || s.equals("true");
    }

    /** Test/audit helper. Snapshot read of the three slot atomics. */
    void inspect(@NotNull UUID uuid, @NotNull BiConsumer<String, TogglePair> visitor) {
        ToggleSlot slot = slots.get(uuid);
        if (slot == null) return;
        visitor.accept(KEY_ALERTS, slot.alerts.get());
        visitor.accept(KEY_VERBOSE, slot.verbose.get());
        visitor.accept(KEY_BRANDS, slot.brands.get());
    }

    @Override
    public void shutdown() {
        if (ownsScheduler) scheduler.shutdownNow();
    }
}
