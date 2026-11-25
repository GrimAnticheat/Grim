package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.event.ServerStartupEvent;
import ac.grim.grimac.api.storage.instance.OwnershipRenewResult;
import ac.grim.grimac.api.storage.instance.OwnershipClaimResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipMetadata;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipGate;
import ac.grim.grimac.api.storage.registry.StoreId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

final class OwnershipHeartbeatScheduler {

    private final @NotNull ServerOwnershipAdapter ownership;
    private final @NotNull StoreId ownershipStore;
    private volatile @NotNull UUID startupId;
    private final @NotNull UUID instanceId;
    private volatile @NotNull UUID fence;
    private final @NotNull String serverName;
    private volatile long startedEpochMs;
    private final @Nullable String hostname;
    private final @Nullable String grimVersion;
    private final @Nullable String serverVersionString;
    private final @NotNull Supplier<byte @Nullable []> verboseManifest;
    private final long initialHeartbeatEpochMs;
    private final long leaseTtlMs;
    private final long safetyMarginMs;
    private final long renewIntervalMs;
    private final @NotNull ServerOwnershipGate gate;
    private final @NotNull Consumer<ServerStartupEvent> publish;
    private final @NotNull Runnable lostOwnership;
    private final @NotNull Logger logger;
    private final AtomicBoolean lost = new AtomicBoolean(false);
    private final AtomicBoolean publishWorkerActive = new AtomicBoolean(false);
    private final AtomicBoolean publishDirty = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean loggedShutdownClosedConnection = new AtomicBoolean(false);
    private final long publishWaitTimeoutMs;

    private final Object lifecycleLock = new Object();
    private @Nullable ScheduledExecutorService renewExecutor;
    private @Nullable ExecutorService publishExecutor;
    private @Nullable ScheduledFuture<?> renewTask;
    private volatile @Nullable Thread renewThread;
    private volatile @Nullable Thread publishThread;
    private volatile long lastSuccessfulRenewEpochMs;
    private volatile long requestedPublishHeartbeatEpochMs;

    OwnershipHeartbeatScheduler(
            @NotNull ServerOwnershipAdapter ownership,
            @NotNull StoreId ownershipStore,
            @NotNull UUID startupId,
            @NotNull UUID instanceId,
            @NotNull UUID fence,
            @NotNull String serverName,
            long startedEpochMs,
            long initialHeartbeatEpochMs,
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
        this.initialHeartbeatEpochMs = initialHeartbeatEpochMs;
        this.hostname = hostname;
        this.grimVersion = grimVersion;
        this.serverVersionString = serverVersionString;
        this.verboseManifest = verboseManifest;
        this.leaseTtlMs = leaseTtlMs;
        this.safetyMarginMs = safetyMarginMs;
        this.renewIntervalMs = Math.max(1L, interval.toMillis());
        this.gate = gate;
        this.publish = publish;
        this.lostOwnership = lostOwnership;
        this.logger = logger;
        this.publishWaitTimeoutMs = publishWaitTimeoutMs(leaseTtlMs, safetyMarginMs);
        this.lastSuccessfulRenewEpochMs = initialHeartbeatEpochMs;
        this.requestedPublishHeartbeatEpochMs = initialHeartbeatEpochMs;
    }

    private @Nullable Consumer<OwnershipClaimResult> recoveryHandler;
    private Runnable recoveryCompleted = () -> {};
    private @Nullable OwnershipClaimResult pendingRecovery;
    private volatile @Nullable UUID recoveryStartupId;
    private @Nullable UUID recoveryFence;
    private @Nullable ServerOwnershipSnapshot previousRecoveryOwner;

    OwnershipHeartbeatScheduler withRecovery(@NotNull Consumer<OwnershipClaimResult> recoveryHandler) {
        this.recoveryHandler = recoveryHandler;
        return this;
    }

    OwnershipHeartbeatScheduler afterRecovery(Runnable recoveryCompleted) {
        this.recoveryCompleted = recoveryCompleted;
        return this;
    }

    void start() {
        synchronized (lifecycleLock) {
            if (renewExecutor != null || publishExecutor != null) return;
            publishExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "grim-storage-ownership-publish-" + serverName);
                t.setDaemon(true);
                publishThread = t;
                return t;
            });
            renewExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "grim-storage-ownership-renew-" + serverName);
                t.setDaemon(true);
                renewThread = t;
                return t;
            });
            renewTask = renewExecutor.scheduleAtFixedRate(this::renewTick, 0L, renewIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    void beginShutdown() {
        synchronized (lifecycleLock) {
            shuttingDown.set(true);
        }
    }

    void publishNowAndWait() {
        ExecutorService current;
        synchronized (lifecycleLock) {
            current = publishExecutor;
        }
        if (current == null || lost.get()) return;
        if (Thread.currentThread() == renewThread) {
            requestPublish(lastSuccessfulRenewEpochMs);
            return;
        }
        if (Thread.currentThread() == publishThread) {
            publishOnce(lastSuccessfulRenewEpochMs);
            return;
        }
        Future<?> future;
        try {
            future = current.submit(() -> publishOnce(lastSuccessfulRenewEpochMs));
        } catch (RejectedExecutionException e) {
            if (!lost.get()) {
                logger.log(Level.WARNING, "ownership heartbeat publish was rejected for startup " + startupId, e);
            }
            return;
        }
        try {
            future.get(publishWaitTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (!logShutdownClosedConnectionOnce("ownership heartbeat publish", e.getCause())) {
                logger.log(Level.WARNING, "ownership heartbeat publish failed for startup " + startupId, e.getCause());
            }
        } catch (CancellationException e) {
            if (!lost.get()) {
                logger.log(Level.WARNING, "ownership heartbeat publish was cancelled for startup " + startupId, e);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warning("ownership heartbeat publish timed out after " + publishWaitTimeoutMs
                    + "ms for startup " + startupId
                    + "; continuing without a synchronous startup manifest barrier"
                    + describePublishThread());
        }
    }

    private static long publishWaitTimeoutMs(long leaseTtlMs, long safetyMarginMs) {
        long leaseBudgetMs = leaseTtlMs - safetyMarginMs;
        if (leaseBudgetMs <= 0L) return 5_000L;
        return Math.max(1_000L, Math.min(10_000L, leaseBudgetMs));
    }

    private @NotNull String describePublishThread() {
        Thread thread = publishThread;
        if (thread == null) return "";
        StringBuilder builder = new StringBuilder();
        builder.append("\nThread \"").append(thread.getName()).append("\" id=")
                .append(thread.getId()).append(" state=").append(thread.getState());
        for (StackTraceElement element : thread.getStackTrace()) {
            builder.append('\n').append("    at ").append(element);
        }
        return builder.toString();
    }

    void stop() {
        synchronized (lifecycleLock) {
            shuttingDown.set(true);
            if (renewTask != null) {
                renewTask.cancel(false);
                renewTask = null;
            }
            if (renewExecutor != null) {
                cancelDrained(renewExecutor.shutdownNow());
                renewExecutor = null;
                renewThread = null;
            }
            if (publishExecutor != null) {
                cancelDrained(publishExecutor.shutdownNow());
                publishExecutor = null;
                publishThread = null;
            }
            publishWorkerActive.set(false);
            publishDirty.set(false);
        }
    }

    private static void cancelDrained(@NotNull List<Runnable> drained) {
        for (Runnable runnable : drained) {
            if (runnable instanceof Future<?> future) {
                future.cancel(false);
            }
        }
    }

    private void renewTick() {
        if (lost.get() || shuttingDown.get()) return;
        try {
            if (recoveryStartupId != null) {
                recoverOwnership();
                return;
            }
            OwnershipRenewResult renewed = ownership.renewOwnership(
                    ownershipStore, instanceId, startupId, fence, leaseTtlMs);
            if (!renewed.renewed()) {
                if (recoveryHandler == null) {
                    onLostOwnership(renewed);
                } else {
                    gate.close("ownership-recovery");
                    recoveryStartupId = UUID.randomUUID();
                    recoveryFence = UUID.randomUUID();
                    logger.warning("[grim-datastore] ownership renewal was rejected; pausing persistence and attempting recovery for instanceId=" + instanceId);
                    recoverOwnership();
                }
                return;
            }
            synchronized (lifecycleLock) {
                if (shuttingDown.get()) return;
                lastSuccessfulRenewEpochMs = renewed.dbNowEpochMs();
                gate.extend(startupId, fence, leaseTtlMs, safetyMarginMs);
            }
            requestPublish(renewed.dbNowEpochMs());
        } catch (RuntimeException e) {
            if (!logShutdownClosedConnectionOnce("ownership heartbeat renew", e)) {
                logger.log(Level.WARNING, "ownership heartbeat failed for startup " + startupId, e);
            }
        } catch (Exception e) {
            if (!logShutdownClosedConnectionOnce("ownership heartbeat renew", e)) {
                logger.log(Level.WARNING, "ownership heartbeat failed for startup " + startupId, e);
            }
        }
    }

    private void recoverOwnership() throws Exception {
        gate.close("ownership-recovery");
        // Never compete with a different boot of a copied data folder. The claim
        // below is atomic; its previous owner also detects a takeover between
        // this read and the claim.
        ServerOwnershipSnapshot owner = ownership.readOwnership(ownershipStore, instanceId).orElse(null);
        if (owner != null && !isRecoveryOwner(owner)) {
            onLostOwnership(OwnershipRenewResult.lost(instanceId, startupId, fence, ownership.dbNowEpochMs()));
            return;
        }
        long dbNow = ownership.dbNowEpochMs();
        boolean existingCandidate = owner != null && recoveryStartupId.equals(owner.ownerStartupId());
        boolean candidateActive = existingCandidate && owner.activeAt(dbNow);
        if (existingCandidate && !candidateActive) {
            // A peer can already have scheduled repair for this unsuccessful
            // candidate. Do not attach live sessions to that expired startup.
            previousRecoveryOwner = owner;
            recoveryStartupId = UUID.randomUUID();
            recoveryFence = UUID.randomUUID();
            pendingRecovery = null;
        }
        // An active candidate is already ours, possibly from an ambiguous
        // claim response. Do not claim it again: it could expire between the
        // read and claim, which would revive an identity peers may be repairing.
        // The final renew below remains the authority for resuming writes.
        OwnershipClaimResult claim = candidateActive
                ? OwnershipClaimResult.claimed(instanceId, recoveryStartupId, recoveryFence,
                        dbNow, owner.leaseExpiresAtEpochMs(), owner)
                : ownership.claimOwnership(ownershipStore, instanceId,
                        recoveryStartupId, recoveryFence, leaseTtlMs,
                        new ServerOwnershipMetadata(serverName, hostname, grimVersion, serverVersionString));
        if (!claim.claimed() || (claim.previousOwner() != null && !isRecoveryOwner(claim.previousOwner()))) {
            try {
                if (claim.claimed()) {
                    ownership.closeOwnership(ownershipStore, instanceId, claim.startupId(), claim.fence(), "recovery-conflict");
                }
            } finally {
                onLostOwnership(OwnershipRenewResult.lost(instanceId, startupId, fence, claim.dbNowEpochMs()));
            }
            return;
        }
        if (shuttingDown.get()) return;
        // Reuse an unexpired candidate after an ambiguous commit or a failed
        // startup publish. Old sessions must never be reopened: peers
        // may already have repaired them while this lease was expired.
        if (pendingRecovery == null) pendingRecovery = claim;
        publish.accept(new ServerStartupEvent()
                .startupId(claim.startupId()).instanceId(instanceId).serverName(serverName)
                .startedEpochMs(pendingRecovery.dbNowEpochMs()).lastHeartbeatEpochMs(claim.dbNowEpochMs())
                .hostname(hostname).grimVersion(grimVersion).serverVersionString(serverVersionString)
                .verboseManifest(verboseManifest.get()));
        recoveryHandler.accept(pendingRecovery);
        // Publishing and session reassignment may block past the TTL. Revalidate before admitting writes.
        OwnershipRenewResult renewed = ownership.renewOwnership(ownershipStore, instanceId,
                claim.startupId(), claim.fence(), leaseTtlMs);
        if (!renewed.renewed()) return;
        synchronized (lifecycleLock) {
            if (shuttingDown.get()) return;
            startupId = claim.startupId();
            fence = claim.fence();
            startedEpochMs = pendingRecovery.dbNowEpochMs();
            lastSuccessfulRenewEpochMs = renewed.dbNowEpochMs();
            gate.open(startupId, fence, leaseTtlMs, safetyMarginMs);
        }
        try {
            recoveryCompleted.run();
        } catch (RuntimeException | Error failure) {
            gate.close("incomplete-session-recovery");
            throw failure;
        }
        synchronized (lifecycleLock) {
            if (shuttingDown.get()) return;
            // Until completion succeeds, the next tick must retry recovery,
            // even if ordinary renewal could still extend the candidate lease.
            pendingRecovery = null;
            recoveryStartupId = null;
            recoveryFence = null;
            previousRecoveryOwner = null;
        }
        requestPublish(renewed.dbNowEpochMs());
        logger.info("[grim-datastore] database persistence resumed with fresh startupId=" + startupId
                + "; events dropped during the outage are not replayed");
    }

    private boolean isRecoveryOwner(ServerOwnershipSnapshot owner) {
        return (startupId.equals(owner.ownerStartupId()) && fence.equals(owner.fence()))
                || (recoveryStartupId.equals(owner.ownerStartupId()) && recoveryFence.equals(owner.fence()))
                || (previousRecoveryOwner != null
                    && previousRecoveryOwner.ownerStartupId().equals(owner.ownerStartupId())
                    && previousRecoveryOwner.fence().equals(owner.fence()));
    }

    private void requestPublish(long heartbeatEpochMs) {
        if (lost.get()) return;
        requestedPublishHeartbeatEpochMs = heartbeatEpochMs;
        publishDirty.set(true);
        ExecutorService current;
        synchronized (lifecycleLock) {
            current = publishExecutor;
        }
        if (current == null) return;
        if (!publishWorkerActive.compareAndSet(false, true)) return;
        submitPublishWorker(current);
    }

    private void submitPublishWorker(@NotNull ExecutorService current) {
        try {
            current.submit(this::drainPublishRequests);
        } catch (RejectedExecutionException e) {
            publishWorkerActive.set(false);
            if (!lost.get()) {
                logger.log(Level.WARNING, "ownership heartbeat publish worker was rejected for startup " + startupId, e);
            }
        }
    }

    private void drainPublishRequests() {
        try {
            while (!lost.get() && publishDirty.getAndSet(false)) {
                publishOnce(requestedPublishHeartbeatEpochMs);
            }
        } finally {
            publishWorkerActive.set(false);
            if (!lost.get() && publishDirty.get()) {
                ExecutorService current;
                synchronized (lifecycleLock) {
                    current = publishExecutor;
                }
                if (current != null && publishWorkerActive.compareAndSet(false, true)) {
                    submitPublishWorker(current);
                }
            }
        }
    }

    private void publishOnce(long heartbeatEpochMs) {
        UUID publishingStartup;
        long publishingStarted;
        synchronized (lifecycleLock) {
            if (lost.get() || recoveryStartupId != null) return;
            publishingStartup = startupId;
            publishingStarted = startedEpochMs;
        }
        try {
            ServerStartupEvent event = new ServerStartupEvent()
                    .startupId(publishingStartup)
                    .instanceId(instanceId)
                    .serverName(serverName)
                    .startedEpochMs(publishingStarted)
                    .lastHeartbeatEpochMs(heartbeatEpochMs)
                    .hostname(hostname)
                    .grimVersion(grimVersion)
                    .serverVersionString(serverVersionString)
                    .verboseManifest(verboseManifest.get());
            publish.accept(event);
        } catch (RuntimeException e) {
            if (!logShutdownClosedConnectionOnce("ownership heartbeat publish", e)) {
                logger.log(Level.WARNING, "ownership heartbeat publish failed for startup " + startupId, e);
            }
        } catch (Exception e) {
            if (!logShutdownClosedConnectionOnce("ownership heartbeat publish", e)) {
                logger.log(Level.WARNING, "ownership heartbeat publish failed for startup " + startupId, e);
            }
        }
    }

    private boolean logShutdownClosedConnectionOnce(@NotNull String action, @Nullable Throwable failure) {
        if (!shuttingDown.get() || !ShutdownDiagnostics.isClosedConnectionFailure(failure)) return false;
        if (loggedShutdownClosedConnection.compareAndSet(false, true)) {
            logger.info("[grim-datastore] " + action + " skipped during shutdown because storage connection is closed: "
                    + ShutdownDiagnostics.rootMessage(failure));
        }
        return true;
    }

    private void onLostOwnership(@NotNull OwnershipRenewResult renewed) {
        if (!lost.compareAndSet(false, true)) return;
        gate.close("lost-ownership");
        logger.warning(lostOwnershipMessage(renewed));
        try {
            lostOwnership.run();
        } finally {
            stop();
        }
    }

    private @NotNull String lostOwnershipMessage(@NotNull OwnershipRenewResult renewed) {
        long dbNow = renewed.dbNowEpochMs();
        long lastRenew = lastSuccessfulRenewEpochMs;
        long renewLateByMs = lastRenew <= 0L ? -1L : Math.max(0L, dbNow - (lastRenew + renewIntervalMs));
        long leaseExpiredByMs = lastRenew <= 0L ? -1L : Math.max(0L, dbNow - (lastRenew + leaseTtlMs));
        long millisSinceClaim = initialHeartbeatEpochMs <= 0L ? -1L : Math.max(0L, dbNow - initialHeartbeatEpochMs);
        return "[grim-datastore] lost DB ownership for instanceId=" + instanceId
                + " startupId=" + startupId
                + "; disabling persistence for this boot"
                + " dbNowEpochMs=" + dbNow
                + " lastSuccessfulRenewEpochMs=" + lastRenew
                + " initialHeartbeatEpochMs=" + initialHeartbeatEpochMs
                + " leaseTtlMs=" + leaseTtlMs
                + " renewIntervalMs=" + renewIntervalMs
                + " millisSinceClaim=" + millisSinceClaim
                + " renewLateByMs=" + renewLateByMs
                + " leaseExpiredByMs=" + leaseExpiredByMs;
    }
}
