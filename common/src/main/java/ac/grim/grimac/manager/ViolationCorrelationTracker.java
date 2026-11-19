package ac.grim.grimac.manager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Bounded, observe-only aggregation of recent check signals. */
public final class ViolationCorrelationTracker {
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final int MAX_SIGNALS = 64;

    private final ArrayDeque<Signal> recent = new ArrayDeque<>(MAX_SIGNALS);

    public synchronized void record(@NotNull String stableKey) {
        record(stableKey, System.nanoTime());
    }

    synchronized void record(@NotNull String stableKey, long nowNanos) {
        Family family = familyFor(stableKey);
        if (family == null) return;

        prune(nowNanos);
        if (recent.size() == MAX_SIGNALS) recent.removeFirst();
        recent.addLast(new Signal(family, stableKey, nowNanos));
    }

    public synchronized @NotNull Snapshot snapshot() {
        return snapshot(System.nanoTime());
    }

    synchronized @NotNull Snapshot snapshot(long nowNanos) {
        prune(nowNanos);
        EnumMap<Family, Integer> counts = new EnumMap<>(Family.class);
        EnumMap<Family, Set<String>> checks = new EnumMap<>(Family.class);
        for (Signal signal : recent) {
            counts.merge(signal.family, 1, Integer::sum);
            checks.computeIfAbsent(signal.family, ignored -> new LinkedHashSet<>()).add(signal.checkKey);
        }
        return new Snapshot(recent.size(), counts, checks);
    }

    private void prune(long nowNanos) {
        while (!recent.isEmpty() && nowNanos - recent.peekFirst().recordedAtNanos >= WINDOW_NANOS) {
            recent.removeFirst();
        }
    }

    private static @Nullable Family familyFor(String stableKey) {
        if (!stableKey.startsWith("grim.")) return null;
        if ("grim.chat.moving_while_chatting".equals(stableKey)) return Family.MOVEMENT;
        int separator = stableKey.indexOf('.', 5);
        if (separator < 0) return null;
        String checkGroup = stableKey.substring(5, separator);
        return switch (checkGroup) {
            case "movement", "flight", "groundspoof", "noslow", "elytra", "vehicle", "timer", "sprint",
                    "prediction", "phase", "velocity" -> Family.MOVEMENT;
            case "combat", "aim", "baritone", "multiinteract" -> Family.COMBAT;
            case "badpackets", "packetorder", "multiactions", "crash", "exploit", "chat", "ping", "post" -> Family.PACKET;
            case "breaking", "scaffolding", "blockplace" -> Family.WORLD;
            default -> null;
        };
    }

    public enum Family {
        MOVEMENT,
        COMBAT,
        PACKET,
        WORLD
    }

    public record Snapshot(int totalSignals, @NotNull Map<Family, Integer> counts,
                           @NotNull Map<Family, Set<String>> checks) {
        public Snapshot {
            counts = Map.copyOf(counts);
            EnumMap<Family, Set<String>> immutableChecks = new EnumMap<>(Family.class);
            checks.forEach((family, keys) -> immutableChecks.put(family, Set.copyOf(keys)));
            checks = Map.copyOf(immutableChecks);
        }

        public int familyCount() {
            return counts.size();
        }
    }

    private record Signal(Family family, String checkKey, long recordedAtNanos) {}
}
