package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.event.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionPublicationConcurrencyTest {
    @Test
    void lookupHeartbeatUpdatesAndQuitDoNotWaitForAnotherPublisher() throws Exception {
        try (Fixture f = new Fixture()) {
            UUID session = f.tracker.observeActivity(f.player, 100, meta(null, 765));
            f.blockNext.set(true);
            Future<UUID> publishing = f.executor.submit(() -> f.tracker.observeActivity(f.player, 200, meta("fabric", 765)));
            assertTrue(f.entered.await(2, TimeUnit.SECONDS));

            assertEquals(session, f.executor.submit(() -> f.tracker.currentSessionId(f.player)).get(1, TimeUnit.SECONDS));
            f.executor.submit(() -> f.tracker.pollHeartbeat(f.player, 201)).get(1, TimeUnit.SECONDS);
            assertEquals(session, f.executor.submit(() -> f.tracker.observeActivity(f.player, 300, meta(null, 766))).get(1, TimeUnit.SECONDS));
            f.executor.submit(() -> f.tracker.close(f.player, 500, SessionTracker.ClientMeta.empty())).get(1, TimeUnit.SECONDS);
            assertNull(f.tracker.currentSessionId(f.player));
            UUID rejoined = f.executor.submit(() -> f.tracker.observeActivity(f.player, 600, meta("new", 767))).get(1, TimeUnit.SECONDS);
            assertNotEquals(session, rejoined);

            f.release.countDown();
            assertEquals(session, publishing.get(2, TimeUnit.SECONDS));
            SessionEvent lastOld = f.writes.stream().filter(event -> event.sessionId().equals(session)).reduce((a, b) -> b).orElseThrow();
            assertEquals(500, lastOld.closedAtEpochMs());
            assertEquals(300, lastOld.lastActivityEpochMs());
            assertEquals("fabric", lastOld.clientBrand());
            assertEquals(766, lastOld.clientVersion());
            assertEquals(rejoined, f.tracker.currentSessionId(f.player));
        }
    }

    @Test
    void recoveryDoesNotWaitForPublisherAndLateSlotUsesRecoveredOwner() throws Exception {
        try (Fixture f = new Fixture()) {
            UUID session = f.tracker.observeActivity(f.player, 100, meta("fabric", 765));
            f.blockNext.set(true);
            Future<UUID> publishing = f.executor.submit(() -> f.tracker.observeActivity(f.player, 200, meta(null, 766)));
            assertTrue(f.entered.await(2, TimeUnit.SECONDS));
            f.writable.set(false);
            UUID recoveredOwner = UUID.randomUUID();
            Runnable finish = f.executor.submit(() -> f.tracker.prepareResume(recoveredOwner, (id, previous, next) -> {
                assertEquals(session, id);
                assertTrue(previous.contains(f.initialOwner));
            })).get(1, TimeUnit.SECONDS);
            f.writable.set(true);
            f.executor.submit(finish).get(1, TimeUnit.SECONDS);
            f.release.countDown();
            publishing.get(2, TimeUnit.SECONDS);
            assertTrue(f.writes.size() >= 2);
            for (SessionEvent event : f.writes.subList(1, f.writes.size())) {
                assertEquals(recoveredOwner, event.startupId());
                assertEquals(session, event.sessionId());
                assertEquals("fabric", event.clientBrand());
                assertEquals(766, event.clientVersion());
            }
        }
    }

    @Test
    void heartbeatBeforeIntervalDoesNotSubmitAndConcurrentDuePollsSubmitOnce() throws Exception {
        try (Fixture f = new Fixture()) {
            f.tracker.observeActivity(f.player, 100, meta("fabric", 765));
            for (int time = 101; time < 1_100; time++) f.tracker.pollHeartbeat(f.player, time);
            assertEquals(1, f.submissions.get());
            CountDownLatch start = new CountDownLatch(1);
            Future<?> first = f.executor.submit(() -> { await(start); f.tracker.pollHeartbeat(f.player, 1_100); });
            Future<?> second = f.executor.submit(() -> { await(start); f.tracker.pollHeartbeat(f.player, 1_100); });
            start.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
            assertEquals(2, f.submissions.get());
        }
    }

    @Test
    void failedSubmissionDoesNotLeavePublisherPermanentlyBusy() throws Exception {
        try (Fixture f = new Fixture()) {
            UUID session = f.tracker.observeActivity(f.player, 100, meta("fabric", 765));
            f.failNext.set(true);
            assertThrows(IllegalStateException.class, () -> f.tracker.observeActivity(f.player, 200, meta(null, 766)));
            f.tracker.close(f.player, 500, SessionTracker.ClientMeta.empty());
            SessionEvent closed = f.writes.get(f.writes.size() - 1);
            assertEquals(session, closed.sessionId());
            assertEquals(500, closed.closedAtEpochMs());
            assertEquals(766, closed.clientVersion());
        }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(2, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    private static SessionTracker.ClientMeta meta(String brand, int version) {
        return new SessionTracker.ClientMeta("test", brand, version, "test");
    }

    private static final class Fixture implements AutoCloseable {
        final UUID player = UUID.randomUUID();
        final UUID initialOwner = UUID.randomUUID();
        final AtomicBoolean writable = new AtomicBoolean(true);
        final AtomicBoolean blockNext = new AtomicBoolean();
        final AtomicBoolean failNext = new AtomicBoolean();
        final AtomicInteger submissions = new AtomicInteger();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final List<SessionEvent> writes = new CopyOnWriteArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final SessionTrackerImpl tracker;

        Fixture() {
            DataStore store = mock(DataStore.class);
            doAnswer(call -> {
                submissions.incrementAndGet();
                if (failNext.compareAndSet(true, false)) throw new IllegalStateException("submission failed");
                if (blockNext.compareAndSet(true, false)) {
                    entered.countDown();
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                }
                SessionEvent event = new SessionEvent();
                Consumer<SessionEvent> fill = call.getArgument(1);
                fill.accept(event);
                writes.add(event);
                return null;
            }).when(store).submit(eq(Categories.SESSION), any());
            tracker = new SessionTrackerImpl(store, "test", 1_000, initialOwner, writable::get);
        }

        @Override
        public void close() throws Exception {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
