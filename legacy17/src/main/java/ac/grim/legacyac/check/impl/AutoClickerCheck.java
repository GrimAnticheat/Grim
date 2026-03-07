package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class AutoClickerCheck extends Check {
    private final Map<UUID, ClickPattern> patterns = new ConcurrentHashMap<UUID, ClickPattern>();

    public AutoClickerCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AutoClicker");
    }

    public void onInteract(PlayerInteractEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        if (isExempt(event.getPlayer(), data)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        long now = System.currentTimeMillis();
        if (data.getClickWindowStart() == 0L || now - data.getClickWindowStart() > 1000L) {
            data.setClickWindowStart(now);
            data.setClickWindow(0);
        }
        data.setClickWindow(data.getClickWindow() + 1);
        int cps = data.getClickWindow();
        ClickPattern pattern = getPattern(event.getPlayer().getUniqueId());
        if (pattern.lastClickAt > 0L) {
            long interval = now - pattern.lastClickAt;
            if (interval > 0L && interval < 1000L) {
                pattern.intervals.addLast(Long.valueOf(interval));
                while (pattern.intervals.size() > 10) {
                    pattern.intervals.removeFirst();
                }
            }
        }
        pattern.lastClickAt = now;

        boolean flatPattern = isFlatPattern(pattern.intervals, plugin.getConfig().getLong("checks.AutoClicker.max-flat-jitter-ms", 2L));
        int maxCps = plugin.getConfig().getInt("checks.AutoClicker.max-cps", 17);
        if (cps > maxCps || flatPattern) {
            double add = cps > maxCps ? 0.6D : 0.35D;
            double buffer = increaseBuffer(data, add);
            if (buffer > plugin.getConfig().getDouble("checks.AutoClicker.buffer", 1.5D)) {
                flag(event.getPlayer(), data, add,
                        "cps=" + cps + (flatPattern ? " flat-pattern" : ""));
            }
        } else {
            coolDownScore(data);
        }
    }

    private ClickPattern getPattern(UUID uuid) {
        ClickPattern pattern = patterns.get(uuid);
        if (pattern == null) {
            pattern = new ClickPattern();
            ClickPattern existing = patterns.putIfAbsent(uuid, pattern);
            if (existing != null) {
                pattern = existing;
            }
        }
        return pattern;
    }

    private boolean isFlatPattern(Deque<Long> intervals, long maxJitterMillis) {
        if (intervals.size() < 6) {
            return false;
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Long value : intervals) {
            if (value.longValue() < min) {
                min = value.longValue();
            }
            if (value.longValue() > max) {
                max = value.longValue();
            }
        }
        return max - min <= maxJitterMillis;
    }

    private static final class ClickPattern {
        private final Deque<Long> intervals = new ArrayDeque<Long>();
        private long lastClickAt;
    }
}


