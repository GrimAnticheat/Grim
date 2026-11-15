package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.api.storage.DataStore;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.event.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionContinuityTest {
    private final List<SessionEvent> writes = new CopyOnWriteArrayList<>();
    private final AtomicBoolean writable = new AtomicBoolean(true);
    private final AtomicBoolean pauseAfterNextWrite = new AtomicBoolean();
    private final UUID initialStartup = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    private SessionTrackerImpl tracker() {
        DataStore store = mock(DataStore.class);
        doAnswer(call -> {
            SessionEvent event = new SessionEvent();
            Consumer<SessionEvent> fill = call.getArgument(1);
            fill.accept(event);
            writes.add(event);
            if (pauseAfterNextWrite.compareAndSet(true, false)) writable.set(false);
            return null;
        }).when(store).submit(eq(Categories.SESSION), any());
        return new SessionTrackerImpl(store, "test", 1_000, initialStartup, writable::get);
    }

    @Test
    void quietPlayerResumesSameSessionWithMetadataObservedDuringOutage() throws Exception {
        SessionTrackerImpl tracker = tracker();
        UUID session = tracker.observeActivity(player, 100, SessionTracker.ClientMeta.empty());
        writable.set(false);
        tracker.observeActivity(player, 200, meta("fabric", 765));
        tracker.pollHeartbeat(player, 3_000);
        UUID recoveredStartup = UUID.randomUUID();
        List<UUID> moved = new ArrayList<>();
        Runnable finish = tracker.prepareResume(recoveredStartup, (id, previous, target) -> {
            assertFalse(writable.get());
            assertTrue(previous.contains(initialStartup));
            moved.add(id);
        });
        assertEquals(1, writes.size(), "paused metadata must remain in memory");
        writable.set(true);
        finish.run();
        assertEquals(List.of(session), moved);
        SessionEvent resumed = writes.get(writes.size() - 1);
        assertEquals(session, resumed.sessionId());
        assertEquals(100, resumed.startedEpochMs());
        assertEquals(3_000, resumed.lastActivityEpochMs());
        assertEquals("fabric", resumed.clientBrand());
        assertEquals(765, resumed.clientVersion());
        assertEquals(recoveredStartup, resumed.startupId());
        assertEquals(0, resumed.closedAtEpochMs());
    }

    @Test
    void quitAndReconnectDuringBlockedHandoffDoNotMergeConnections() throws Exception {
        SessionTrackerImpl tracker = tracker();
        UUID oldSession = tracker.observeActivity(player, 100, meta("old", 765));
        writable.set(false);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Runnable> preparing = executor.submit(() -> tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> {
            entered.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS));
        }));
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            tracker.close(player, 500, SessionTracker.ClientMeta.empty());
            UUID newSession = tracker.observeActivity(player, 600, meta("new", 766));
            assertNotEquals(oldSession, newSession);
            release.countDown();
            Runnable finish = preparing.get(2, TimeUnit.SECONDS);
            writable.set(true);
            finish.run();
            SessionEvent closed = writes.stream().filter(e -> e.sessionId().equals(oldSession) && e.closedAtEpochMs() != 0).findFirst().orElseThrow();
            SessionEvent joined = writes.stream().filter(e -> e.sessionId().equals(newSession)).findFirst().orElseThrow();
            assertEquals(500, closed.closedAtEpochMs());
            assertEquals(100, closed.startedEpochMs());
            assertEquals("old", closed.clientBrand());
            assertEquals(600, joined.startedEpochMs());
            assertEquals("new", joined.clientBrand());
            assertEquals(766, joined.clientVersion());
            assertEquals(newSession, tracker.currentSessionId(player));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedHandoffCanRetryWithoutReplacingSessionOrMetadata() throws Exception {
        SessionTrackerImpl tracker = tracker();
        UUID session = tracker.observeActivity(player, 100, meta("fabric", 765));
        writable.set(false);
        UUID failedCandidate = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> tracker.prepareResume(failedCandidate,
                (id, previous, target) -> { throw new IllegalStateException("DB interrupted"); }));
        UUID next = UUID.randomUUID();
        Runnable finish = tracker.prepareResume(next, (id, previous, target) -> {
            assertEquals(session, id);
            assertTrue(previous.contains(initialStartup));
            assertTrue(previous.contains(failedCandidate));
        });
        writable.set(true);
        finish.run();
        assertEquals(session, writes.get(writes.size() - 1).sessionId());
        assertEquals("fabric", writes.get(writes.size() - 1).clientBrand());
    }

    @Test
    void playerWhoQuitDuringOutageIsNotResumed() throws Exception {
        SessionTrackerImpl tracker = tracker();
        tracker.observeActivity(player, 100, meta("fabric", 765));
        writable.set(false);
        tracker.close(player, 200, SessionTracker.ClientMeta.empty());
        Runnable finish = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> fail("disconnected player must not resume"));
        writable.set(true);
        finish.run();
        assertNull(tracker.currentSessionId(player));
        assertEquals(1, writes.size());
    }

    @Test
    void staleCompletionCannotDiscardPendingQuitAndSuccessfulCompletionReleasesIt() throws Exception {
        SessionTrackerImpl tracker = tracker();
        UUID session = tracker.observeActivity(player, 100, meta("fabric", 765));
        writable.set(false);
        Runnable stale = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> {});
        tracker.close(player, 500, SessionTracker.ClientMeta.empty());
        Runnable retry = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> fail("quit must not reopen"));
        writable.set(true);
        stale.run();
        assertEquals(1, writes.size());
        retry.run();
        assertEquals(2, writes.size());
        assertEquals(session, writes.get(1).sessionId());
        assertEquals(500, writes.get(1).closedAtEpochMs());
        writable.set(false);
        Runnable later = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> fail("no active connections"));
        writable.set(true);
        later.run();
        assertEquals(2, writes.size(), "completed closures must not remain retained across later recoveries");
    }

    @Test
    void completionInterruptedDuringFlushRetainsAllPendingQuits() throws Exception {
        SessionTrackerImpl tracker = tracker();
        UUID otherPlayer = UUID.randomUUID();
        UUID first = tracker.observeActivity(player, 100, meta("fabric", 765));
        UUID second = tracker.observeActivity(otherPlayer, 200, meta("vanilla", 766));
        writable.set(false);
        Runnable completion = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> {});
        tracker.close(player, 500, SessionTracker.ClientMeta.empty());
        tracker.close(otherPlayer, 600, SessionTracker.ClientMeta.empty());
        writable.set(true);
        pauseAfterNextWrite.set(true);
        assertThrows(IllegalStateException.class, completion::run);
        assertEquals(3, writes.size(), "only the first pending close was submitted before the gate closed");
        Runnable retry = tracker.prepareResume(UUID.randomUUID(), (id, previous, target) -> fail("quit must not reopen"));
        writable.set(true);
        retry.run();
        assertEquals(5, writes.size(), "both terminal states survive partial completion");
        assertTrue(writes.stream().anyMatch(event -> event.sessionId().equals(first) && event.closedAtEpochMs() == 500));
        assertTrue(writes.stream().anyMatch(event -> event.sessionId().equals(second) && event.closedAtEpochMs() == 600));
    }

    private static SessionTracker.ClientMeta meta(String brand, int version) {
        return new SessionTracker.ClientMeta("test", brand, version, "test");
    }
}
