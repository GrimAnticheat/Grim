package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.event.ServerStartupEvent;
import ac.grim.grimac.api.storage.instance.OwnershipRenewResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipGate;
import ac.grim.grimac.api.storage.registry.StoreId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

final class OwnershipHeartbeatScheduler {

    private final @NotNull ServerOwnershipAdapter ownership;
    private final @NotNull StoreId ownershipStore;
    private final @NotNull UUID startupId;
    private final @NotNull UUID instanceId;
    private final @NotNull UUID fence;
    private final @NotNull String serverName;
    private final long startedEpochMs;
    private final @Nullable String hostname;
    private final @Nullable String grimVersion;
    private final @Nullable String serverVersionString;
    private final @NotNull Supplier<byte @Nullable []> verboseManifest;
    private final long leaseTtlMs;
    private final long safetyMarginMs;
    private final @NotNull Duration interval;
    private final @NotNull ServerOwnershipGate gate;
    private final @NotNull Consumer<ServerStartupEvent> publish;
    private final @NotNull Runnable lostOwnership;
    private final @NotNull Logger logger;
    private final AtomicBoolean lost = new AtomicBoolean(false);

    private final Object lifecycleLock = new Object();
    private @Nullable ScheduledExecutorService executor;
    private @Nullable ScheduledFuture<?> task;
    private volatile @Nullable Thread schedulerThread;

    OwnershipHeartbeatScheduler(
            @NotNull ServerOwnershipAdapter ownership,
            @NotNull StoreId ownershipStore,
            @NotNull UUID startupId,
            @NotNull UUID instanceId,
            @NotNull UUID fence,
            @NotNull String serverName,
            long startedEpochMs,
            @Nullable String hostname,
            @Nullable String grimVersion,
            @Nullable String serverVersionString,
            @NotNull Supplier<byte @Nullable []> verboseManifest,
            long leaseTtlMs,
            long safetyMarginMs,
            @NotNull Duration interval,
            @NotNull ServerOwnershipGate gate,
            @NotNull Consumer<ServerStartupEvent> publish,
            @NotNull Runnable lostOwnership,
            @NotNull Logger logger) {
        this.ownership = ownership;
        this.ownershipStore = ownershipStore;
        this.startupId = startupId;
        this.instanceId = instanceId;
        this.fence = fence;
        this.serverName = serverName;
        this.startedEpochMs = startedEpochMs;
        this.hostname = hostname;
        this.grimVersion = grimVersion;
        this.serverVersionString = serverVersionString;
        this.verboseManifest = verboseManifest;
        this.leaseTtlMs = leaseTtlMs;
        this.safetyMarginMs = safetyMarginMs;
        this.interval = interval;
        this.gate = gate;
        this.publish = publish;
        this.lostOwnership = lostOwnership;
        this.logger = logger;
    }

    void start() {
        synchronized (lifecycleLock) {
            if (executor != null) return;
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "grim-storage-ownership-" + serverName);
                t.setDaemon(true);
                schedulerThread = t;
                return t;
            });
            long periodMs = Math.max(1L, interval.toMillis());
            task = executor.scheduleAtFixedRate(this::tick, 0L, periodMs, TimeUnit.MILLISECONDS);
        }
    }

    void publishNowAndWait() {
        ScheduledExecutorService current;
        synchronized (lifecycleLock) {
            current = executor;
        }
        if (current == null || lost.get()) return;
        if (Thread.currentThread() == schedulerThread) {
            tick();
            return;
        }
        Future<?> future = current.submit(this::tick);
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "ownership heartbeat failed for startup " + startupId, e.getCause());
        }
    }

    void stop() {
        synchronized (lifecycleLock) {
            if (task != null) {
                task.cancel(false);
                task = null;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
                schedulerThread = null;
            }
        }
    }

    private void tick() {
        if (lost.get()) return;
        try {
            OwnershipRenewResult renewed = ownership.renewOwnership(
                    ownershipStore, instanceId, startupId, fence, leaseTtlMs);
            if (!renewed.renewed()) {
                onLostOwnership();
                return;
            }
            gate.extend(startupId, fence, leaseTtlMs, safetyMarginMs);
            ServerStartupEvent event = new ServerStartupEvent()
                    .startupId(startupId)
                    .instanceId(instanceId)
                    .serverName(serverName)
                    .startedEpochMs(startedEpochMs)
                    .lastHeartbeatEpochMs(renewed.dbNowEpochMs())
                    .hostname(hostname)
                    .grimVersion(grimVersion)
                    .serverVersionString(serverVersionString)
                    .verboseManifest(verboseManifest.get());
            publish.accept(event);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "ownership heartbeat failed for startup " + startupId, e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "ownership heartbeat failed for startup " + startupId, e);
        }
    }

    private void onLostOwnership() {
        if (!lost.compareAndSet(false, true)) return;
        gate.close("lost-ownership");
        logger.warning("[grim-datastore] lost DB ownership for instanceId=" + instanceId
                + " startupId=" + startupId + "; disabling persistence for this boot");
        try {
            lostOwnership.run();
        } finally {
            stop();
        }
    }
}
