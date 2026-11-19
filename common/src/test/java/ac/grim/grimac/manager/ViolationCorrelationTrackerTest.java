package ac.grim.grimac.manager;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViolationCorrelationTrackerTest {
    @Test
    void aggregatesIndependentFamiliesWithinTheWindow() {
        ViolationCorrelationTracker tracker = new ViolationCorrelationTracker();
        long start = 1_000L;

        tracker.record("grim.combat.reach", start);
        tracker.record("grim.aim.modulo", start + 1);
        tracker.record("grim.multiinteract.multiple_targets", start + 2);
        tracker.record("grim.packetorder.invalid", start + 3);

        ViolationCorrelationTracker.Snapshot snapshot = tracker.snapshot(start + 4);
        assertEquals(4, snapshot.totalSignals());
        assertEquals(2, snapshot.familyCount());
        assertEquals(3, snapshot.counts().get(ViolationCorrelationTracker.Family.COMBAT));
        assertEquals(3, snapshot.checks().get(ViolationCorrelationTracker.Family.COMBAT).size());
        assertEquals(1, snapshot.counts().get(ViolationCorrelationTracker.Family.PACKET));
    }

    @Test
    void expiresOldSignalsAndIgnoresUnclassifiedChecks() {
        ViolationCorrelationTracker tracker = new ViolationCorrelationTracker();
        long start = 1_000L;

        tracker.record("grim.combat.reach", start);
        tracker.record("grim.misc.client_brand", start + 1);

        ViolationCorrelationTracker.Snapshot snapshot =
                tracker.snapshot(start + TimeUnit.SECONDS.toNanos(10));
        assertEquals(0, snapshot.totalSignals());
        assertEquals(0, snapshot.familyCount());
    }

    @Test
    void keepsTheMostRecentSignalsWithinItsBound() {
        ViolationCorrelationTracker tracker = new ViolationCorrelationTracker();
        for (int i = 0; i < 70; i++) {
            tracker.record("grim.combat.reach", i);
        }

        assertEquals(64, tracker.snapshot(70).totalSignals());
    }
}
