package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.backend.BackendException;
import ac.grim.grimac.api.storage.instance.OwnershipClaimResult;
import ac.grim.grimac.api.storage.instance.OwnershipRenewResult;
import ac.grim.grimac.api.storage.instance.ServerOwnershipAdapter;
import ac.grim.grimac.api.storage.instance.ServerOwnershipGate;
import ac.grim.grimac.api.storage.instance.ServerOwnershipMetadata;
import ac.grim.grimac.api.storage.instance.ServerOwnershipSnapshot;
import ac.grim.grimac.api.storage.registry.StoreId;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnershipHeartbeatSchedulerTest {
    private static final Logger LOGGER = Logger.getLogger(OwnershipHeartbeatSchedulerTest.class.getName());

    @Test
    void synchronousOwnershipPublishTimesOutWhenPublishWorkerIsBlocked() throws Exception {
        UUID instanceId = UUID.randomUUID();
        UUID startupId = UUID.randomUUID();
        UUID fence = UUID.randomUUID();
        ServerOwnershipGate gate = new ServerOwnershipGate(true);
        gate.open(startupId, fence, 20_000L, 5_000L);

        CountDownLatch publishEntered = new CountDownLatch(1);
        CountDownLatch releasePublish = new CountDownLatch(1);
        OwnershipHeartbeatScheduler scheduler = new OwnershipHeartbeatScheduler(
                new RenewingOwnershipAdapter(),
                StoreId.grim("server_ownership"),
                startupId,
                instanceId,
                fence,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                "test",
                "test",
                () -> null,
                20L,
                19L,
                Duration.ofDays(1),
                gate,
                event -> {
                    publishEntered.countDown();
                    try {
                        releasePublish.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                () -> {},
                LOGGER);

        try {
            scheduler.start();
            assertTrue(publishEntered.await(2, TimeUnit.SECONDS));

            long start = System.nanoTime();
            scheduler.publishNowAndWait();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertTrue(elapsedMs < 2_500L, "publishNowAndWait should time out instead of blocking indefinitely");
        } finally {
            releasePublish.countDown();
            scheduler.stop();
        }
    }

    @Test
    void blockedManifestPublishDoesNotStarveOwnershipRenewals() throws Exception {
        UUID instanceId = UUID.randomUUID();
        UUID startupId = UUID.randomUUID();
        UUID fence = UUID.randomUUID();
        ServerOwnershipGate gate = new ServerOwnershipGate(true);
        long leaseTtlMs = 500L;
        long renewIntervalMs = 50L;
        gate.open(startupId, fence, leaseTtlMs, 100L);

        ExpiringOwnershipAdapter ownership = new ExpiringOwnershipAdapter(leaseTtlMs);
        CountDownLatch publishEntered = new CountDownLatch(1);
        CountDownLatch releasePublish = new CountDownLatch(1);
        AtomicInteger lostOwnershipCount = new AtomicInteger();
        OwnershipHeartbeatScheduler scheduler = new OwnershipHeartbeatScheduler(
                ownership,
                StoreId.grim("server_ownership"),
                startupId,
                instanceId,
                fence,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                "test",
                "test",
                () -> null,
                leaseTtlMs,
                100L,
                Duration.ofMillis(renewIntervalMs),
                gate,
                event -> {
                    publishEntered.countDown();
                    try {
                        releasePublish.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                lostOwnershipCount::incrementAndGet,
                LOGGER);

        try {
            scheduler.start();
            assertTrue(publishEntered.await(2, TimeUnit.SECONDS));
            int renewsWhenPublishBlocked = ownership.renewCount();

            Thread.sleep(leaseTtlMs + 300L);

            assertTrue(ownership.renewCount() > renewsWhenPublishBlocked,
                    "renewals should continue while manifest publish is blocked");
            assertEquals(0, lostOwnershipCount.get());
            assertTrue(gate.allowWrites());
        } finally {
            releasePublish.countDown();
            scheduler.stop();
        }
    }

    @Test
    void synchronousOwnershipPublishReturnsWhenLostOwnershipStopsBlockedPublishExecutor() throws Exception {
        UUID instanceId = UUID.randomUUID();
        UUID startupId = UUID.randomUUID();
        UUID fence = UUID.randomUUID();
        ServerOwnershipGate gate = new ServerOwnershipGate(true);
        gate.open(startupId, fence, 20_000L, 5_000L);

        CountDownLatch renewEntered = new CountDownLatch(1);
        CountDownLatch releaseRenew = new CountDownLatch(1);
        CountDownLatch publishEntered = new CountDownLatch(1);
        CountDownLatch releasePublish = new CountDownLatch(1);
        CountDownLatch lostOwnershipRan = new CountDownLatch(1);
        OwnershipHeartbeatScheduler scheduler = new OwnershipHeartbeatScheduler(
                new BlockingLostOwnershipAdapter(renewEntered, releaseRenew),
                StoreId.grim("server_ownership"),
                startupId,
                instanceId,
                fence,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                "test",
                "test",
                () -> null,
                20_000L,
                5_000L,
                Duration.ofDays(1),
                gate,
                event -> {
                    publishEntered.countDown();
                    try {
                        releasePublish.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                lostOwnershipRan::countDown,
                LOGGER);

        Thread runningPublishThread = new Thread(scheduler::publishNowAndWait, "test-ownership-publish-running");
        runningPublishThread.setDaemon(true);
        Thread queuedPublishThread = new Thread(scheduler::publishNowAndWait, "test-ownership-publish-queued");
        queuedPublishThread.setDaemon(true);

        try {
            scheduler.start();
            assertTrue(renewEntered.await(2, TimeUnit.SECONDS));
            runningPublishThread.start();
            assertTrue(publishEntered.await(2, TimeUnit.SECONDS));
            queuedPublishThread.start();
            assertTrue(waitUntilWaiting(queuedPublishThread, 2_000L));

            releaseRenew.countDown();

            runningPublishThread.join(2_000L);
            queuedPublishThread.join(2_000L);
            assertFalse(runningPublishThread.isAlive(), "running publish should be interrupted when ownership loss stops the scheduler");
            assertFalse(queuedPublishThread.isAlive(), "queued publish should be cancelled when ownership loss stops the scheduler");
            assertTrue(lostOwnershipRan.await(1, TimeUnit.SECONDS));
        } finally {
            releaseRenew.countDown();
            releasePublish.countDown();
            scheduler.stop();
        }
    }

    @Test
    void shutdownClosedConnectionPublishFailureDoesNotWarnWithStackTrace() {
        UUID instanceId = UUID.randomUUID();
        UUID startupId = UUID.randomUUID();
        UUID fence = UUID.randomUUID();
        ServerOwnershipGate gate = new ServerOwnershipGate(true);
        gate.open(startupId, fence, 20_000L, 5_000L);

        Logger logger = Logger.getLogger("ownership-shutdown-quiet-test-" + UUID.randomUUID());
        CapturingHandler handler = new CapturingHandler();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

        OwnershipHeartbeatScheduler scheduler = new OwnershipHeartbeatScheduler(
                new RenewingOwnershipAdapter(),
                StoreId.grim("server_ownership"),
                startupId,
                instanceId,
                fence,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                "test",
                "test",
                () -> null,
                20_000L,
                5_000L,
                Duration.ofDays(1),
                gate,
                event -> {
                    throw new RuntimeException("server startup write failed",
                            new SQLException("Connection is closed"));
                },
                () -> {},
                logger);

        try {
            scheduler.beginShutdown();
            scheduler.start();
            scheduler.publishNowAndWait();
        } finally {
            scheduler.stop();
            logger.removeHandler(handler);
        }

        assertEquals(0, handler.warningCount(), "shutdown closed-connection publish should not log a WARN stack trace");
        assertEquals(1, handler.infoCount(), "shutdown closed-connection publish should log one quiet line");
    }

    private static boolean waitUntilWaiting(Thread thread, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }

    private static class RenewingOwnershipAdapter implements ServerOwnershipAdapter {
        @Override
        public void ensureStore(StoreId id) {
        }

        @Override
        public long dbNowEpochMs() {
            return System.currentTimeMillis();
        }

        @Override
        public OwnershipClaimResult claimOwnership(
                StoreId id,
                UUID persistentId,
                UUID startupId,
                UUID fence,
                long ttlMs,
                ServerOwnershipMetadata metadata) {
            long now = dbNowEpochMs();
            return OwnershipClaimResult.claimed(persistentId, startupId, fence, now, now + ttlMs, null);
        }

        @Override
        public OwnershipRenewResult renewOwnership(
                StoreId id,
                UUID persistentId,
                UUID startupId,
                UUID fence,
                long ttlMs) {
            long now = dbNowEpochMs();
            return OwnershipRenewResult.renewed(persistentId, startupId, fence, now, now + ttlMs);
        }

        @Override
        public boolean closeOwnership(
                StoreId id,
                UUID persistentId,
                UUID startupId,
                UUID fence,
                String reason) {
            return true;
        }

        @Override
        public Optional<ServerOwnershipSnapshot> readOwnership(StoreId id, UUID persistentId) throws BackendException {
            return Optional.empty();
        }
    }

    private static final class ExpiringOwnershipAdapter extends RenewingOwnershipAdapter {
        private final AtomicInteger renewCount = new AtomicInteger();
        private volatile long leaseExpiresAtMs;

        private ExpiringOwnershipAdapter(long ttlMs) {
            this.leaseExpiresAtMs = System.currentTimeMillis() + ttlMs;
        }

        int renewCount() {
            return renewCount.get();
        }

        @Override
        public OwnershipRenewResult renewOwnership(
                StoreId id,
                UUID persistentId,
                UUID startupId,
                UUID fence,
                long ttlMs) {
            long now = dbNowEpochMs();
            if (now >= leaseExpiresAtMs) {
                return OwnershipRenewResult.lost(persistentId, startupId, fence, now);
            }
            renewCount.incrementAndGet();
            leaseExpiresAtMs = now + ttlMs;
            return OwnershipRenewResult.renewed(persistentId, startupId, fence, now, leaseExpiresAtMs);
        }
    }

    private static final class BlockingLostOwnershipAdapter extends RenewingOwnershipAdapter {
        private final CountDownLatch renewEntered;
        private final CountDownLatch releaseRenew;

        private BlockingLostOwnershipAdapter(CountDownLatch renewEntered, CountDownLatch releaseRenew) {
            this.renewEntered = renewEntered;
            this.releaseRenew = releaseRenew;
        }

        @Override
        public OwnershipRenewResult renewOwnership(
                StoreId id,
                UUID persistentId,
                UUID startupId,
                UUID fence,
                long ttlMs) {
            renewEntered.countDown();
            try {
                releaseRenew.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return OwnershipRenewResult.lost(persistentId, startupId, fence, dbNowEpochMs());
        }
    }

    private static final class CapturingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public synchronized void publish(LogRecord record) {
            records.add(record);
        }

        @Override public void flush() {}
        @Override public void close() throws SecurityException {}

        synchronized int warningCount() {
            return (int) records.stream()
                    .filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
                    .count();
        }

        synchronized int infoCount() {
            return (int) records.stream()
                    .filter(record -> record.getLevel() == Level.INFO)
                    .count();
        }
    }
}
